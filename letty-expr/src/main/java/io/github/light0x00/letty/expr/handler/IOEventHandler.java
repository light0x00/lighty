package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.ChannelConfigurationProvider;
import io.github.light0x00.letty.expr.LettyConfig;
import io.github.light0x00.letty.expr.buffer.BufferPool;
import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.expr.buffer.RingByteBuffer;
import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.eventloop.EventLoop;
import io.github.light0x00.letty.expr.eventloop.EventLoopGroup;
import io.github.light0x00.letty.expr.eventloop.NioEventLoop;
import io.github.light0x00.letty.expr.handler.adapter.ChannelHandler;
import io.github.light0x00.letty.expr.util.LettyException;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 基于责任链和观察者模式分发底层事件
 * <p>
 * handle the nio event and dispatch them to {@link ChannelHandler}
 *
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
public class IOEventHandler implements EventHandler {

    protected final NioEventLoop eventLoop;

    protected final SelectionKey key;

    protected final ChannelContext context;

    protected final BufferPool bufferPool;

    protected final LettyConfig lettyConf;

    protected final SocketChannel javaChannel;

    @Getter
    protected final NioSocketChannel channel;

    /**
     * 对方是否发送 FIN 包, 这意味着已经读完了 socket 读缓冲区的最后一个字节
     */
    private volatile boolean inboundFIN;

    /**
     * 当前端是否发送了 FIN 包, 这意味着不会再写入更多数据到 socket 写缓冲区
     */
    private volatile boolean outboundFIN;

    /**
     * 握手成功后触发
     */
    @Getter
    protected final ListenableFutureTask<Void> connectedFuture = new ListenableFutureTask<>(null);
    /**
     * 两端关闭时,即两阶段回收完成时触发
     */
    @Getter
    protected final ListenableFutureTask<Void> closedFuture = new ListenableFutureTask<>(null);

    /**
     * 目前存在的竞争条件:
     * <p>
     * 事件线程/用户线程 {@link #shutdownOutput()} 执行:
     *
     * <pre>
     * outboundFIN = true;
     * clear
     * </pre>
     * <p>
     * 用户线程 {@link #write(Object, ListenableFutureTask)} 执行:
     *
     * <pre>
     * if outboundFIN == false
     * then offer
     * </pre>
     * <p>
     * 事件线程 {@link #processWritableEvent()} 执行:
     * <pre>
     * while(not empty)
     *      poll
     * </pre>
     * <p>
     * TODO: 可采用读写锁优化
     * <li> {@link #write(Object, ListenableFutureTask)} {@link #processWritableEvent()} 可以并发执行, 使用读锁
     * <li> {@link #shutdownOutput()} 需要排他执行,使用写锁
     */
    @GuardedBy("outboundLock")
    private final Queue<BufferFuturePair> outboundBuffer = new LinkedList<>();

    private final Object outboundLock = new Object();

    /**
     * 用于执行责任链和观察者
     */
    protected final EventLoop handlerExecutor;

    protected final IOPipelineChain ioChain;

    protected final ChannelEventNotifier eventNotifier;

    public IOEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, ChannelConfigurationProvider configProvider) {
        this.eventLoop = eventLoop;
        this.javaChannel = channel;
        this.key = key;

        this.channel = channelDecorator();

        //context
        context = buildContext();

        var configuration = configProvider.configuration(this.channel);

        lettyConf = configuration.lettyConf();
        bufferPool = configuration.bufferPool();

        //executor
        EventLoopGroup<?> handlerExecutorGroup = configuration.handlerExecutor();
        if (handlerExecutorGroup == null || eventLoop.group() == handlerExecutorGroup) {
            handlerExecutor = eventLoop;
        } else {
            handlerExecutor = handlerExecutorGroup.next();
        }

        var inboundHandlers = configuration.inboundHandlers();
        var outboundHandlers = configuration.outboundHandlers();

        //init responsibility chain
        ioChain = new IOPipelineChain(eventLoop, context, inboundHandlers, outboundHandlers, this::write);

        //init notifier for observers
        eventNotifier = new ChannelEventNotifier(handlerExecutor, inboundHandlers, outboundHandlers);
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
        key.interestOps((key.interestOps() ^ SelectionKey.OP_CONNECT) | SelectionKey.OP_READ);

        connectedFuture.run();
        eventNotifier.onConnected(context);
    }

    private void processReadableEvent() throws IOException {
        int n;
        do {
            RecyclableByteBuffer buf = bufferPool.take(lettyConf.readBufSize());
            n = buf.readFromChannel(javaChannel);
            if (handlerExecutor.inEventLoop()) {
                ioChain.input(buf);
            } else {
                handlerExecutor.execute(() -> ioChain.input(buf));
            }
        } while (n > 0);
        if (n == -1) {
            log.debug("Received FIN from {}", javaChannel.getRemoteAddress());

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
                buf.writeToChannel(javaChannel);
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

        RingByteBuffer rBuf;
        if (data instanceof RingByteBuffer) {
            rBuf = (RingByteBuffer) data;
        } else if (data instanceof ByteBuffer bBuf) {
            rBuf = new RingByteBuffer(bBuf, bBuf.position(), bBuf.limit(), bBuf.limit());
        } else if (data.getClass().equals(byte[].class)) {
            ByteBuffer bBuf = ByteBuffer.wrap((byte[]) data);
            rBuf = new RingByteBuffer(bBuf, bBuf.position(), bBuf.limit(), bBuf.limit());
        } else {
            throw new LettyException("Unsupported data type to write:" + data.getClass());
        }

        synchronized (outboundLock) {
            if (outboundFIN) {
                throw new ClosedChannelException();
            }

            rBuf.writeToChannel(javaChannel);
            if (rBuf.remainingCanGet() > 0) {
                outboundBuffer.offer(new BufferFuturePair(rBuf, writeFuture));
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
        return writeFuture;
    }

    @SneakyThrows
    private void shutdownInput() {
        //这个调用会触发一个读事件, read 返回 -1
        //之后对方发送的数据都会收到 RST
        javaChannel.shutdownInput();
    }

    @SneakyThrows
    private void shutdownOutput() {
        log.debug("Be going to shutdown output...");
        synchronized (outboundLock) {
            outboundFIN = true;

            log.debug("Remove OP_WRITE from interest set");
            key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);

            log.debug("Clear outbound buffer ,remaining:{}", outboundBuffer.size());
            for (BufferFuturePair bufFuture; (bufFuture = outboundBuffer.poll()) != null; ) {
                bufFuture.future.cancel(false);
            }

            javaChannel.shutdownOutput();
            log.debug("Send FIN to {}", javaChannel.getRemoteAddress());
        }
    }

    @SneakyThrows
    private void close() {
        shutdownOutput();

        javaChannel.close();
        key.cancel();

        eventNotifier.onClosed(context);
        closedFuture.setSuccess();
    }

    private NioSocketChannelImpl channelDecorator() {
        return new NioSocketChannelImpl(javaChannel) {
            @NotNull
            @Override
            public ListenableFutureTask<Void> closeFuture() {
                return closedFuture;
            }

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

    @NotNull
    private ChannelContext buildContext() {
        return new ChannelContext() {

            @NotNull
            @Override
            public NioSocketChannel channel() {
                return channel;
            }

            @NotNull
            @Override
            public RecyclableByteBuffer allocateBuffer(int capacity) {
                return bufferPool.take(capacity);
            }
        };
    }

    private record BufferFuturePair(RingByteBuffer buffer, ListenableFutureTask<Void> future) {

    }
}
