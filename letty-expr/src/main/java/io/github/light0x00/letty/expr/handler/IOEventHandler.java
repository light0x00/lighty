package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.ChannelConfigurationProvider;
import io.github.light0x00.letty.expr.LettyConfig;
import io.github.light0x00.letty.expr.NioSocketChannel;
import io.github.light0x00.letty.expr.buffer.BufferPool;
import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.expr.buffer.RingByteBuffer;
import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.eventloop.EventLoop;
import io.github.light0x00.letty.expr.eventloop.EventLoopGroup;
import io.github.light0x00.letty.expr.eventloop.NioEventLoop;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
public class IOEventHandler implements EventHandler {

    final NioEventLoop eventLoop;

    final SelectionKey key;

    ChannelContext context;

    BufferPool bufferPool;

    LettyConfig lettyConf;

    SocketChannel channel;

    NioSocketChannel channelWrapper;

    EventLoop executor;

    /**
     * 当前端是否发送了 FIN 包, 这意味着不会再写入更多数据到 socket 写缓冲区
     */
    volatile boolean inboundFIN;

    /**
     * 对方是否发送 FIN 包, 这意味着已经读完了 socket 读缓冲区的最后一个字节
     */
    volatile boolean outboundFIN;

    /**
     * 握手成功后触发
     */
    @Getter
    ListenableFutureTask<Void> connectedFuture = new ListenableFutureTask<>(null);
    /**
     * 两端关闭时,即两阶段回收完成时触发
     */
    @Getter
    ListenableFutureTask<Void> closedFuture = new ListenableFutureTask<>(null);

    /**
     * 目前存在的竞争条件:
     *
     * 事件线程/用户线程 {@link #shutdownOutput()} 执行:
     *
     * <pre>
     * outboundFIN = true;
     * clear
     * </pre>
     *
     * 用户线程 {@link #write(Object, ListenableFutureTask)} 执行:
     *
     * <pre>
     * if outboundFIN == false
     * then offer
     * </pre>
     *
     * 事件线程 {@link #processWritableEvent()} 执行:
     * <pre>
     * while(not empty)
     *      poll
     * </pre>
     *
     * TODO: 可采用读写锁优化
     * <li> {@link #write(Object, ListenableFutureTask)} {@link #processWritableEvent()} 可以并发执行, 使用读锁
     * <li> {@link #shutdownOutput()} 需要排他执行,使用写锁
     *
     */
    @GuardedBy("outboundBuffer")
    final Queue<BufferFuturePair> outboundBuffer = new LinkedList<>();

    final Object outboundLock = new Object();

    IOPipelineChain ioChain;

    ChannelEventNotifier eventNotifier;

    public IOEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, ChannelConfigurationProvider configProvider) {
        this.eventLoop = eventLoop;
        this.channel = channel;
        this.key = key;

        channelWrapper = new NioSocketChannel(channel);

        var configuration = configProvider.configuration(channelWrapper);
        init(configuration);
    }

    private void init(ChannelConfiguration configuration) {
        lettyConf = configuration.lettyConf();
        bufferPool = configuration.bufferPool();

        //executor
        EventLoopGroup<?> handlerExecutorGroup = configuration.handlerExecutor();
        if (eventLoop.group() == handlerExecutorGroup) {
            executor = eventLoop;
        } else {
            executor = handlerExecutorGroup.next();
        }

        //context
        context = buildContext();

        var inboundPipelines = configuration.inboundHandlers();
        var outboundPipelines = configuration.outboundHandlers();

        //init responsibility chain
        ioChain = new IOPipelineChain(context, inboundPipelines, outboundPipelines, this::write);

        //init observer
        eventNotifier = new ChannelEventNotifier(inboundPipelines, outboundPipelines);

    }

    @Override
    public void onEvent(SelectionKey key) throws IOException {
        if (key.isReadable()) {
            processReadableEvent();
        } else if (key.isWritable()) {
            processWritableEvent();
        } else if (key.isConnectable()) {
            processConnectableEvent();
        }
    }

    private void processConnectableEvent() throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        channel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);

        connectedFuture.run();
        eventNotifier.onConnected(context);
    }

    private void processReadableEvent() throws IOException {
        int n;
        do {
            RecyclableByteBuffer buf = bufferPool.take(lettyConf.readBufSize());
            n = buf.readFromChannel(channel);
            if (executor.inEventLoop()) {
                ioChain.input(buf);
            } else {
                executor.execute(() -> ioChain.input(buf));
            }
        } while (n > 0);
        if (n == -1) {
            log.debug("Received FIN from {}", channel.getRemoteAddress());

            inboundFIN = true;
            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ); //remove read from interestOps

            eventNotifier.onReadCompleted(context);

            if (!lettyConf.isAllowHalfClosure()) {
                close();
            }
        }
    }

    private void processWritableEvent() throws IOException {
        synchronized (outboundLock) {
            for (BufferFuturePair bufFuture; (bufFuture = outboundBuffer.peek()) != null; ) {

                RingByteBuffer buf = bufFuture.buffer();
                /*
                 * Some types of channels, depending upon their state,
                 * may write only some of the bytes or possibly none at all.
                 * A socket channel in non-blocking mode, for example,
                 * cannot write any more bytes than are free in the socket's output buffer.
                 */
                buf.writeToChannel(channel);
                if (buf.remainingCanGet() == 0) {
                    outboundBuffer.poll();
                    bufFuture.future.run();
                } else {
                    //如果还有剩余，意味 socket 发送缓冲着已经满了，只能等待下一次 Writable 事件
                    log.debug("suspend writing socket buffer");
                    return;
                }
            }

            //remove write from interestOps
            key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
            log.debug("Outbound buffer has been flushed, remove interest to writeable event.");
        }
    }

    @SneakyThrows
    private ListenableFutureTask<Void> write(Object data, ListenableFutureTask<Void> writeFuture) {
        if (!(data instanceof RingByteBuffer buf)) {
            throw new ClassCastException("Unsupported data type to write:" + data.getClass());
        }
        synchronized (outboundLock) {
            if (outboundFIN) {
                throw new ClosedChannelException();
            }

            buf.writeToChannel(channel);
            if (buf.remainingCanGet() > 0) {
                outboundBuffer.offer(new BufferFuturePair(buf, writeFuture));
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
        return writeFuture;
    }

    @SneakyThrows
    private void shutdownInput() {
        channel.shutdownInput();
    }

    @SneakyThrows
    private void shutdownOutput() {
        synchronized (outboundLock) {
            outboundFIN = true;
            key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);

            log.debug("Outbound buffer remaining:{}", outboundBuffer.size());

            for (BufferFuturePair bufFuture; (bufFuture = outboundBuffer.poll()) != null; ) {
                bufFuture.future.cancel(false);
            }

            log.debug("Send FIN to {}", channel.getRemoteAddress());
        }
    }

    @SneakyThrows
    private void close() {

        shutdownOutput();

        channel.close();
        key.cancel();

        eventNotifier.onClosed(context);
        closedFuture.run();
    }

    @NotNull
    private ChannelContext buildContext() {
        return new ChannelContext() {

            @NotNull
            @Override
            public ListenableFutureTask<Void> shutdownInput() {
                return eventLoop.submit(IOEventHandler.this::shutdownInput);
            }

            @NotNull
            @Override
            public ListenableFutureTask<Void> shutdownOutput() {
                return eventLoop.submit(IOEventHandler.this::shutdownOutput);
            }

            @NotNull
            @Override
            public RecyclableByteBuffer allocateBuffer(int capacity) {
                return bufferPool.take(capacity);
            }

            @NotNull
            @Override
            public ListenableFutureTask<Void> close() {
                eventLoop.execute(IOEventHandler.this::close);
                return closedFuture;
            }

            @Override
            public ListenableFutureTask<Void> write(@NotNull Object data) {
                return ioChain.output(data);
            }
        };
    }

    private record BufferFuturePair(RingByteBuffer buffer, ListenableFutureTask<Void> future) {

    }
}
