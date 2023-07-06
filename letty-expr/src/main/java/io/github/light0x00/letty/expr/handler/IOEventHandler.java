package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.ChannelContext;
import io.github.light0x00.letty.expr.LettyConf;
import io.github.light0x00.letty.expr.ListenableFutureTask;
import io.github.light0x00.letty.expr.RingByteBuffer;
import io.github.light0x00.letty.expr.buffer.BufferPool;
import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.expr.eventloop.EventExecutor;
import io.github.light0x00.letty.expr.eventloop.EventExecutorGroup;
import io.github.light0x00.letty.expr.eventloop.NioEventLoop;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    LettyConf lettyConf;

    SocketChannel channel;

    EventExecutor executor;

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
    ListenableFutureTask<Void> closeFuture = new ListenableFutureTask<>(null);

    /**
     * NIO 线程 {@link #processReadableEvent()} 更新 closedByPeer \ closed
     * <p>
     * close 执行清除
     * 用户线程 {@link #write(Object, ListenableFutureTask)} 执行添加
     * NIO 线程 {@link #processWritableEvent()} 执行移除
     */
    final Queue<BufferFuturePair> buffersToWrite = new ConcurrentLinkedDeque<>();

    InboundPipelineInvocation inboundInvocation;

    OutboundInvocation outboundInvocation;

    Runnable connectedNotifier;

    Runnable readCompletedNotifier;

    Runnable closedNotifier;

    public IOEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, ChannelHandlerConfigurer configurer) {
        this.eventLoop = eventLoop;
        this.channel = channel;
        this.key = key;
        init(configurer);
    }

    private void init(ChannelHandlerConfigurer configurer) {
        lettyConf = configurer.lettyConf();
        bufferPool = configurer.bufferPool();

        //executor
        EventExecutorGroup<?> executorGroup = configurer.executorGroup();
        if (eventLoop.group() == executorGroup) {
            executor = eventLoop;
        } else {
            executor = executorGroup.next();
        }

        //context
        context = new ChannelContext() {

            @NotNull
            @Override
            public RecyclableByteBuffer allocateBuffer(int capacity) {
                return bufferPool.take(capacity);
            }

            @NotNull
            @Override
            public ListenableFutureTask<Void> close() {
                return closeFuture;
            }

            @Override
            public ListenableFutureTask<Void> write(@NotNull Object data) {
                var writeFuture = new ListenableFutureTask<Void>(null);
                outboundInvocation.invoke(data, writeFuture);
                return writeFuture;
            }
        };

        //pipeline
        List<InboundChannelHandler> inboundPipelines = configurer.inboundHandlers();
        List<OutboundChannelHandler> outboundPipelines = configurer.outboundHandlers();

        inboundInvocation = InboundPipelineInvocation.buildInvocationChain(
                context, inboundPipelines);

        outboundInvocation = OutboundInvocation.buildInvocationChain(
                context, outboundPipelines, this::write);

        //observers
        Set<ChannelObserver> observers = Stream.concat(inboundPipelines.stream(), outboundPipelines.stream())
                .collect(Collectors.toSet());  //去重,主要是针对同时实现了 inbound、outbound 接口的 handler

        connectedNotifier = buildNotifier(observers, (ob) -> ob.onConnected(context));
        readCompletedNotifier = buildNotifier(observers, (ob) -> ob.onReadCompleted(context));
        closedNotifier = buildNotifier(observers, (ob) -> ob.onClosed(context));
    }

    private Runnable buildNotifier(Set<ChannelObserver> observers, Consumer<ChannelObserver> handle) {
        //TODO
        //1. Skip 注解, 未重写的观察者跳过不通知 + 去重
        //2. 异常捕获,避免一个观察者的bug导致其他观察者无法被通知
        return new Runnable() {
            @Override
            public void run() {
                for (ChannelObserver observer : observers) {
                    if (executor.inEventLoop()) {
                        handle.accept(observer);
                    } else {
                        executor.execute(() -> handle.accept(observer));
                    }
                }
            }
        };
    }

    @SneakyThrows
    @Override
    public void onEvent(SelectionKey key) {
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
        connectedNotifier.run();
    }

    private void processReadableEvent() throws IOException {
        int n;
        do {
            RecyclableByteBuffer buf = bufferPool.take(lettyConf.readBufSize());
            n = buf.readFromChannel(channel);
            if (executor.inEventLoop()) {
                inboundInvocation.invoke(buf);
            } else {
                executor.execute(() -> inboundInvocation.invoke(buf));
            }
        } while (n > 0);
        if (n == -1) {
            log.debug("Received FIN from {}", channel.getRemoteAddress());

            //对方调用了 close
            inboundFIN = true;
            //检查写缓冲区是否写完, 如写完直接关闭, 否则 writable 事件中写完后再关闭
            closeIfNoPendingWrite();
        }
    }

    private void processWritableEvent() throws IOException {

        for (BufferFuturePair bufFuture; (bufFuture = buffersToWrite.peek()) != null; ) {

            RingByteBuffer buf = bufFuture.buffer();
            /*
             * Some types of channels, depending upon their state,
             * may write only some of the bytes or possibly none at all.
             * A socket channel in non-blocking mode, for example,
             * cannot write any more bytes than are free in the socket's output buffer.
             */
            buf.writeToChannel(channel);
            if (buf.remainingCanGet() == 0) {
                buffersToWrite.poll();
                bufFuture.future.run();
            } else {
                //如果还有剩余，意味 socket 发送缓冲着已经满了，只能等待下一次 Writable 事件
                log.debug("suspend writing socket buffer");
                return;
            }
        }

        if (inboundFIN) {
            closeIfNoPendingWrite();
        } else {
            removeWritableEventInterestIfNoPendingWrite();
        }
    }

    private ListenableFutureTask<Void> write(Object data, ListenableFutureTask<Void> writeFuture) {
        if (data instanceof RingByteBuffer buf) {
            pendingWriteIfNotClosed(writeFuture, buf);
        } else if (data instanceof ByteBuffer buf) {
            pendingWriteIfNotClosed(writeFuture, new RingByteBuffer(buf));
        } else {
            throw new ClassCastException("Unsupported data type to write:" + data.getClass());
        }
        return writeFuture;
    }

    /**
     * 如果写缓冲区为空,则关闭当前端,即 socket 出方向
     */
    private void closeIfNoPendingWrite() throws IOException {
        synchronized (buffersToWrite) {
            if (buffersToWrite.isEmpty()) {
                log.debug("send FIN to {}", channel.getRemoteAddress());
                outboundFIN = true;
                channel.close();
                key.cancel();
            }
        }
    }

    /**
     * 如果当前端未关闭,即 socket 出方向未关闭,则追加写到写缓冲区
     */
    private void pendingWriteIfNotClosed(ListenableFutureTask<Void> writeFuture, RingByteBuffer buf) {
        synchronized (buffersToWrite) {
            if (outboundFIN) {
                throw new IllegalStateException("Socket Closed");
            }
            buffersToWrite.offer(new BufferFuturePair(buf, writeFuture));
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    /**
     * 如果写缓冲区为空,则从 interest set 移除 {@link SelectionKey#OP_WRITE} 事件
     */
    private void removeWritableEventInterestIfNoPendingWrite() {
        synchronized (buffersToWrite) {
            if (buffersToWrite.isEmpty()) {
                //remove write from interestOps
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                log.debug("Pending buffer has been tidy, remove interest to writeable event.");
            }
        }
    }

    private record BufferFuturePair(RingByteBuffer buffer, ListenableFutureTask<Void> future) {

    }
}
