package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.BasicNioSocketChannel;
import io.github.light0x00.letty.core.LettyConfiguration;
import io.github.light0x00.letty.core.LettyProperties;
import io.github.light0x00.letty.core.buffer.BufferPool;
import io.github.light0x00.letty.core.buffer.RecyclableBuffer;
import io.github.light0x00.letty.core.buffer.RingBuffer;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventLoop;
import io.github.light0x00.letty.core.eventloop.EventLoopGroup;
import io.github.light0x00.letty.core.eventloop.NioEventLoop;
import io.github.light0x00.letty.core.handler.adapter.ChannelHandler;
import io.github.light0x00.letty.core.util.LettyException;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

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

    protected final LettyProperties lettyConf;

    protected final SocketChannel javaChannel;

    @Getter
    protected final NioSocketChannel channel;

    private final OutputBuffer outputBuffer = new OutputBuffer();

    /**
     * 对方是否发送 FIN 包, 这意味着已经读完了 socket 读缓冲区的最后一个字节
     *
     * @implNote Be aware that should only be access by current event loop thread,
     * cannot be shared between threads.
     */
    private boolean outputClosed;

    /**
     * 当前端是否发送了 FIN 包, 这意味着不会再写入更多数据到 socket 写缓冲区
     *
     * @implNote Be aware that should only be access by current event loop thread,
     * cannot be shared between threads.
     */
    private boolean inputClosed;

    /**
     * 用于执行责任链和观察者
     */
    protected final EventLoop handlerExecutor;

    protected final IOPipelineChain ioChain;

    protected final ChannelEventNotifier eventNotifier;

    /**
     * 用于反馈握手的结果,成功或失败
     */
    @Getter
    protected final ListenableFutureTask<NioSocketChannel> connectableFuture;

    public IOEventHandler(NioEventLoop eventLoop,
                          SocketChannel channel,
                          SelectionKey key,
                          LettyConfiguration lettyConfiguration) {
        this(eventLoop, channel, key, lettyConfiguration, new ListenableFutureTask<>(null));
    }

    public IOEventHandler(NioEventLoop eventLoop,
                          SocketChannel javaChannel,
                          SelectionKey key,
                          LettyConfiguration configProvider,
                          ListenableFutureTask<NioSocketChannel> connectableFuture
    ) {
        this.eventLoop = eventLoop;
        this.javaChannel = javaChannel;
        this.key = key;
        this.connectableFuture = connectableFuture;
        channel = channelDecorator();
        context = buildContext();
        lettyConf = configProvider.lettyProperties();
        bufferPool = configProvider.bufferPool();
        /*
            Build pipeline and event notifier
         */
        ChannelHandlerConfiguration channelConfiguration = configProvider.handlerConfigurer().configure(new BasicNioSocketChannel(javaChannel));

        //determine executor
        EventLoopGroup<?> handlerExecutorGroup = channelConfiguration.handlerExecutor();
        if (handlerExecutorGroup == null || eventLoop.group() == handlerExecutorGroup) {
            //如果没有单独指定 executor 去执行用户代码, 那么使用当前 event loop 执行.
            handlerExecutor = eventLoop;
        } else {
            handlerExecutor = handlerExecutorGroup.next();
        }

        var inboundHandlers = channelConfiguration.inboundHandlers();
        var outboundHandlers = channelConfiguration.outboundHandlers();

        //init notifier for observers
        eventNotifier = new ChannelEventNotifier(handlerExecutor, inboundHandlers, outboundHandlers);

        //init responsibility chain
        ioChain = new IOPipelineChain(handlerExecutor, context,
                inboundHandlers,
                outboundHandlers,
                //the final phase of the outbound pipeline, executed by current event loop , to avoid the inter-thread race.
                (data, future) -> {
                    if (eventLoop.inEventLoop()) {
                        write(data, future);
                    } else {
                        eventLoop.submit(() -> write(data, future));
                    }
                });
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
        try {
            javaChannel.finishConnect();
        } catch (IOException e) {
            connectableFuture.setFailure(e);
            throw e;
        }
        connectableFuture.setSuccess(channel);
        eventNotifier.onConnected(context);
        key.interestOps((key.interestOps() ^ SelectionKey.OP_CONNECT) | SelectionKey.OP_READ);
    }

    private void processReadableEvent() throws IOException {
        int n;
        do {
            RecyclableBuffer buf = bufferPool.take(lettyConf.readBufSize());
            n = buf.readFromChannel(javaChannel);
            if (handlerExecutor.inEventLoop()) {
                ioChain.input(buf);
            } else {
                handlerExecutor.execute(() -> ioChain.input(buf));
            }
        } while (n > 0);
        if (n == -1) {
            log.debug("Received FIN from {}", javaChannel.getRemoteAddress());

            readCompleted();
        }
    }

    private void processWritableEvent() throws IOException {
        for (BufferFuturePair bufFuture; (bufFuture = outputBuffer.peek()) != null; ) {

            RingBuffer buf = bufFuture.buffer();
            /*
             * Some types of channels, depending upon their state,
             * may write only some of the bytes or possibly none at all.
             * A socket channel in non-blocking mode, for example,
             * cannot write any more bytes than are free in the socket's output buffer.
             */
            buf.writeToChannel(javaChannel);
            if (buf.remainingCanGet() == 0) {
                outputBuffer.poll();
                if (buf instanceof RecyclableBuffer recyclable) {
                    recyclable.release();
                    log.debug("Recycle buffer {}", recyclable);
                }
                bufFuture.future.setSuccess();
            } else {
                //如果还有剩余，意味 socket 发送缓冲着已经满了，只能等待下一次 Writable 事件
                log.debug("suspend writing socket buffer");
                return;
            }
        }

        key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
        log.debug("All the outbound buffer has been written, remove interest to writeable event.");
    }

    /**
     * Race condition with:
     * <li> {@link #shutdownOutput()} , if then act race condition on the mutable state {@link #outputClosed}
     *
     * <p>
     * Be aware that must be executed in curren event loop, cuz there is no any synchronization!
     */
    @SneakyThrows
    private ListenableFutureTask<Void> write(Object data, ListenableFutureTask<Void> writeFuture) {
        if (outputClosed) {
            throw new LettyException("Output closed");
        }

        RingBuffer rBuf;
        if (data instanceof RingBuffer) {
            rBuf = (RingBuffer) data;
        } else if (data instanceof ByteBuffer bBuf) {
            rBuf = new RingBuffer(bBuf, bBuf.position(), bBuf.position(), bBuf.limit(), true);
        } else if (data.getClass().equals(byte[].class)) {
            ByteBuffer bBuf = ByteBuffer.wrap((byte[]) data);
            rBuf = new RingBuffer(bBuf, bBuf.position(), bBuf.position(), bBuf.limit(), true);
        } else {
            throw new LettyException("Unsupported data type to write:" + data.getClass());
        }

        //if output not fin, then write
        if (outputBuffer.isEmpty()) {
            rBuf.writeToChannel(javaChannel);
        }
        if (rBuf.remainingCanGet() > 0) {
            outputBuffer.offer(new BufferFuturePair(rBuf, writeFuture));
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            if (rBuf instanceof RecyclableBuffer recyclable) {
                recyclable.release();
                log.debug("Recycle buffer {}", recyclable);
            }
        }
        return writeFuture;
    }

    private void readCompleted() {
        //移除事件监听
        key.interestOps(key.interestOps() ^ SelectionKey.OP_READ); //remove read from interestOps
        //更新状态
        inputClosed = true;
        //事件通知
        eventNotifier.onReadCompleted(context);

        if (outputClosed) {
            onFinalized();
        } else if (!lettyConf.isAllowHalfClosure()) {
            shutdownOutput();
        }
    }

    @SneakyThrows
    private void shutdownInput() {
        //这个调用会触发一个读事件, read 返回 -1
        //此调用之后对方发送的数据都会收到 RST
        javaChannel.shutdownInput();
    }

    /**
     * Race condition with:
     *
     * <li>{@link #close()} , may concurrently change the share mutable state {@link #outputClosed}
     * <li>{@link #processWritableEvent()} , cannot concurrently execute, may cause a {@link java.nio.channels.CancelledKeyException}
     * <li>{@link #write(Object, ListenableFutureTask)} , if then act race condition on the mutable state {@link #outputClosed}
     *
     * <p>
     * Be aware that must be executed in curren event loop, cuz there is no any synchronization!
     */
    @SneakyThrows
    private void shutdownOutput() {
        //幂等
        if (outputClosed)
            return;
        outputClosed = true;
        //清除输出缓冲
        outputBuffer.invalid();
        //移除监听
        key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
        //关闭底层 socket 的输出
        javaChannel.shutdownOutput();
        log.debug("Send FIN to {}", javaChannel.getRemoteAddress());

        if (inputClosed) {
            onFinalized();
        }
    }

    /**
     * Race condition with:
     *
     * <li> {@link #readCompleted()} , race condition on the mutable state {@link #inputClosed}
     * <li> {@link #shutdownOutput()} , race condition on the mutable state {@link #outputClosed}
     *
     * <p>
     * Be aware that must be executed in curren event loop, cuz there is no any synchronization!
     */
    private void close() {

        //幂等
        if (outputClosed && inputClosed) {
            return;
        }
        //close 的时候有可能已经调用了 shutdownOutput , 也可能没有
        //如果没有, 则需要执行一次清空
        if (!outputClosed) {
            outputBuffer.invalid();
        }
        outputClosed = inputClosed = true;

        onFinalized();
    }

    @SneakyThrows
    private void onFinalized() {
        log.debug("Release resource {}", javaChannel);

        javaChannel.close();
        key.cancel();

        eventNotifier.onClosed(context);
    }

    private BasicNioSocketChannel channelDecorator() {
        return new BasicNioSocketChannel(javaChannel) {

            @NotNull
            @Override
            public ListenableFutureTask<Void> connectedFuture() {
                return IOEventHandler.this.eventNotifier.connectedFuture;
            }

            @NotNull
            @Override
            public ListenableFutureTask<Void> closeFuture() {
                return IOEventHandler.this.eventNotifier.closedFuture;
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
                return closeFuture();
            }

            @Override
            public ListenableFutureTask<Void> write(@Nonnull Object data) {
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
            public RecyclableBuffer allocateBuffer(int capacity) {
                return bufferPool.take(capacity);
            }
        };
    }

    private record BufferFuturePair(RingBuffer buffer, ListenableFutureTask<Void> future) {

    }

    public static class OutputBuffer {
        private final Queue<BufferFuturePair> outputBuffer = new ConcurrentLinkedDeque<>();

        public void offer(BufferFuturePair bf) {
            outputBuffer.offer(bf);
        }

        public BufferFuturePair poll() {
            return outputBuffer.poll();
        }

        public BufferFuturePair peek() {
            return outputBuffer.peek();
        }

        public boolean isEmpty() {
            return outputBuffer.isEmpty();
        }

        public void invalid() {
            log.debug("Clear outbound buffer ,remaining:{}", outputBuffer.size());
            for (BufferFuturePair bufFuture; (bufFuture = outputBuffer.poll()) != null; ) {
                bufFuture.future.setFailure(new LettyException("Output Buffer cleared"));
            }
        }
    }

}
