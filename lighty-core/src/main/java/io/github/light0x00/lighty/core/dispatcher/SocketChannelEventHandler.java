package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.buffer.BufferPool;
import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.buffer.RingBuffer;
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.concurrent.SuccessFutureListener;
import io.github.light0x00.lighty.core.dispatcher.writestrategy.ByteBufferWriteStrategy;
import io.github.light0x00.lighty.core.dispatcher.writestrategy.FileChannelWriteStrategy;
import io.github.light0x00.lighty.core.dispatcher.writestrategy.RingBufferWriteStrategy;
import io.github.light0x00.lighty.core.dispatcher.writestrategy.WriteStrategy;
import io.github.light0x00.lighty.core.eventloop.NioEventLoop;
import io.github.light0x00.lighty.core.facade.*;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.ChannelContextImpl;
import io.github.light0x00.lighty.core.handler.ChannelHandler;
import io.github.light0x00.lighty.core.util.EventLoopConfinement;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Handle the nio event and dispatch them to {@link ChannelHandler}
 *
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
@EventLoopConfinement
public abstract class SocketChannelEventHandler implements NioEventHandler {

    protected final NioEventLoop eventLoop;

    protected final SelectionKey key;

    protected final ChannelContext context;

    protected final BufferPool bufferPool;

    protected final LightyProperties lettyProperties;

    protected final SocketChannel javaChannel;

    @Getter
    protected final NioSocketChannel channel;

    private final OutputBuffer outputBuffer = new OutputBuffer();

    /**
     * 当前端是否发送了 FIN 包, 这意味着不会再写入更多数据到 socket 写缓冲区
     */
    private boolean outputClosed = true;

    /**
     * 对方是否发送 FIN 包, 这意味着已经读完了 socket 读缓冲区的最后一个字节
     */
    private boolean inputClosed = true;

    private boolean destroyed = false;

    protected final ChannelHandlerDispatcher dispatcher;

    /**
     * 用于反馈握手的结果,成功或失败
     */
    @Getter
    protected final ListenableFutureTask<NioSocketChannel> connectableFuture;

    protected int flushThreshold;

    public SocketChannelEventHandler(NioEventLoop eventLoop,
                                     SocketChannel javaChannel,
                                     SelectionKey key,
                                     ChannelInitializer<InitializingNioSocketChannel> channelInitializer,
                                     LightyProperties lettyProperties,
                                     BufferPool bufferPool,
                                     ListenableFutureTask<NioSocketChannel> connectableFuture
    ) {
        this.eventLoop = eventLoop;
        this.javaChannel = javaChannel;
        this.key = key;
        this.connectableFuture = connectableFuture;

        connectableFuture.addListener(new SuccessFutureListener<>((c) -> {
            inputClosed = false;
            outputClosed = false;
            flushThreshold = c.getOption(StandardSocketOptions.SO_SNDBUF);
        }));

        this.lettyProperties = lettyProperties;
        this.bufferPool = bufferPool;

        channel = new NioSocketChannelImpl();
        context = new ChannelContextImpl(channel, bufferPool);

        var channelConfiguration = new InitializingNioSocketChannel(javaChannel, eventLoop);

        channelInitializer.initChannel(channelConfiguration);
        dispatcher = new ChannelHandlerDispatcher(
                context,
                channelConfiguration.eventExecutor(),
                channelConfiguration.handlerExecutorPair(),
                new InboundTailInvocation(),
                new OutboundTailInvocation()
        );

        dispatcher.onInitialize();
    }

    @Override
    public void onEvent(SelectionKey key) throws IOException {
        if (key.isReadable()) {
            processReadableEvent();
        } else if (key.isWritable()) {
            processWritableEvent();
        } else if (key.isConnectable()) {
            processEvent(key);
        }
    }

    public abstract void processEvent(SelectionKey key);

    private void processReadableEvent() throws IOException {
        int n;
        while (true) {
            RecyclableBuffer buf = bufferPool.take(lettyProperties.readBufSize());
            n = buf.readFromChannel(javaChannel);
            if (n > 0)
                dispatcher.input(buf);
            else
                break;
        }

        if (n == -1) {
            log.debug("Received FIN from {}", javaChannel.getRemoteAddress());

            readCompleted();
        }
    }

    private void processWritableEvent() throws IOException {
        for (WriterFuturePair pair; (pair = outputBuffer.peek()) != null; ) {

            WriteStrategy writer = pair.writer();
            /*
             * Some types of channels, depending upon their state,
             * may write only some of the bytes or possibly none at all.
             * A socket channel in non-blocking mode, for example,
             * cannot write any more bytes than are free in the socket's output buffer.
             */
            writer.write(javaChannel);

            if (writer.remaining() == 0) {
                outputBuffer.poll();
                pair.future().setSuccess();
                writer.close();
            } else {
                //如果还有剩余，意味 socket 发送缓冲着已经满了，只能等待下一次 Writable 事件
                log.debug("Underlying socket sending buffer is full, wait for the next time writable event.");
                return;
            }

            //每次写完一个数据,都会触发 future, 用户代码有可能会执行 close/shutdownOutput, 所以这里需要检查  [issue 0x01]
            if (outputClosed) {
                log.debug("Output closed");
                return;
            }
        }

        key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
        log.debug("All the outbound buffers flushed, remove interest to writeable event.");
    }

    /**
     * Race condition with:
     * <li> {@link #shutdownOutput()} , if then act race condition on the mutable state {@link #outputClosed}
     *
     * <p>
     * Be aware that must be executed in curren event loop, cuz there is no any synchronization!
     *
     * @implNote must be executed in current handler's event loop
     */
    @SneakyThrows
    private ListenableFutureTask<Void> write(Object data, ListenableFutureTask<Void> writeFuture, boolean flush) {
        if (outputClosed) {
            writeFuture.setFailure(new LightyException("Channel output closed"));
            return writeFuture;
        }

        WriteStrategy writer;
        if (data instanceof RingBuffer) {
            writer = new RingBufferWriteStrategy((RingBuffer) data);
        } else if (data instanceof ByteBuffer bBuf) {
            writer = new ByteBufferWriteStrategy(bBuf);
        } else if (data.getClass().equals(byte[].class)) {
            byte[] bBuf = (byte[]) data;
            writer = new ByteBufferWriteStrategy(bBuf);
        } else if (data instanceof FileChannel fc) {
            writer = new FileChannelWriteStrategy(fc);
        } else {
            writeFuture.setFailure(new LightyException("Unsupported data type to write:" + data.getClass()));
            return writeFuture;
        }

        if (!flush && (outputBuffer.size() + writer.remaining()) < flushThreshold) {
            outputBuffer.offer(new WriterFuturePair(writer, writeFuture));
            return writeFuture;
        }

        if (outputBuffer.isEmpty()) {
            writer.write(javaChannel);
        }
        if (writer.remaining() > 0) {
            outputBuffer.offer(new WriterFuturePair(writer, writeFuture));
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            writeFuture.setSuccess();
            if (writer.getSource() instanceof RecyclableBuffer recyclable) {
                recyclable.release();
            }
        }
        return writeFuture;
    }

    /**
     * @implNote must be executed in current handler's event loop
     */
    private void flush() {
        if (outputBuffer.size() > 0) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private void readCompleted() {
        //移除事件监听
        key.interestOps(key.interestOps() ^ SelectionKey.OP_READ); //remove read from interestOps
        //更新状态
        inputClosed = true;
        //事件通知
        dispatcher.onReadCompleted();

        if (outputClosed) {
            dispatcher.onClosed();
            destroy();
        } else if (!lettyProperties.isAllowHalfClosure()) {
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
     * <li>{@link #write(Object, ListenableFutureTask, boolean)} , if then act race condition on the mutable state {@link #outputClosed}
     *
     * <p>
     * Be aware that must be executed in curren event loop, cuz there is no any synchronization!
     */
    @SneakyThrows
    private void shutdownOutput() {
        if (outputClosed)
            return;
        outputClosed = true;
        //清除输出缓冲
        outputBuffer.invalid(new LightyException("Channel output is going to shut down"));
        //移除写监听
        key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
        //关闭底层 socket 的输出
        javaChannel.shutdownOutput();
        log.debug("Send FIN to {}", javaChannel.getRemoteAddress());

        if (inputClosed) {
            dispatcher.onClosed();
            destroy();
        }
    }

    /**
     * 如果处于活跃状态(连接已经建立), 则先 close 再 destroy
     * 否则, 直接 destroy
     */
    public ListenableFutureTask<Void> onEventLoopShutdown() {
        if (eventLoop.inEventLoop()) {
            shutdown0();
            return ListenableFutureTask.successFuture();
        } else {
            return eventLoop.submit(this::shutdown0);
        }
    }

    private void shutdown0() {
        if (isClosed()) {
            destroy();
        } else {
            close();
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
    protected void close() {
        if (isClosed()) {
            return;
        }
        //close 的时候有可能已经调用了 shutdownOutput , 也可能没有
        //如果没有, 则需要执行一次清空
        if (!outputClosed) {
            outputBuffer.invalid(new LightyException("Channel is going to close"));
        }
        outputClosed = inputClosed = true;

        dispatcher.onClosed();
        destroy();
    }

    @SneakyThrows
    protected void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;

        String name = javaChannel.toString();

        javaChannel.close();
        key.cancel();

        dispatcher.onDestroy();

        log.debug("Released resources associated with channel {}", name);
    }

    private boolean isClosed() {
        return outputClosed && inputClosed;
    }

    class NioSocketChannelImpl extends AbstractNioSocketChannel {

        public NioSocketChannelImpl() {
            super(javaChannel);
        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> connectedFuture() {
            return SocketChannelEventHandler.this.dispatcher.connectedFuture();
        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> closedFuture() {
            return SocketChannelEventHandler.this.dispatcher.closedFuture();
        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> shutdownInput() {
            eventLoop.execute(SocketChannelEventHandler.this::shutdownInput);
            return SocketChannelEventHandler.this.dispatcher.readCompletedFuture();
        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> shutdownOutput() {
            return eventLoop.submit(SocketChannelEventHandler.this::shutdownOutput);

        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> close() {
            //[issue 0x01]
            //由于这个 close 操作是对外的, 用户代码随时可能调用, 如果调用后立即在当前事件循环 close 的话
            //可能会影响后面还需使用资源的逻辑
            //比如, processWritableEvent 中, 会遍历所有的待写数据, 每写完一个都会执行用户代码(future)
            //如果用户代码调用 close 在当前事件循环就执行, 就会导致遍历到下一个数据时使用一个已关闭的 channel、key
            eventLoop.execute(SocketChannelEventHandler.this::close);
            return closedFuture();
        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> write(@Nonnull Object data) {
            return dispatcher.output(data, false);
        }

        @Nonnull
        @Override
        public ListenableFutureTask<Void> writeAndFlush(@Nonnull Object data) {
            return dispatcher.output(data, true);
        }

        @Override
        public void flush() {
            eventLoop.execute(SocketChannelEventHandler.this::flush);
        }
    }

    @Nonnull
    private ChannelContext buildContext() {
        return new ChannelContextImpl(channel, bufferPool);
    }

    class InboundTailInvocation implements InboundPipelineInvocation {

        @Override
        public void invoke(Object data, ListenableFutureTask<Void> upstreamFuture) {
            String pattern = "Discarded inbound message {} that reached at the tail of the pipeline. Please check your pipeline configuration.";
            log.debug(pattern, data);
            upstreamFuture.setFailure(new LightyException(pattern, data));
        }
    }

    class OutboundTailInvocation implements OutboundPipelineInvocation {

        @Override
        public void invoke(Object data, ListenableFutureTask<Void> future, boolean flush) {
            if (eventLoop.inEventLoop()) {
                write(data, future, flush);
            } else {
                eventLoop.execute(() -> write(data, future, flush));
            }
        }
    }

}
