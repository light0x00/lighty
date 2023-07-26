package io.github.light0x00.letty.core.expr;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public class EventContext {


    //TODO 需要考虑 channel 直接给外部使用的风险; 是否有必要装饰器模式包装一层
    @Getter
    private SocketChannel channel;

    private final SelectionKey selectionKey;

    public final Queue<ByteBuffer> buffersToWrite = new ConcurrentLinkedDeque<>();

    @Getter
    private Invocation decodeInvocation;

    @Getter
    private Invocation encodeInvocation;

    private final MessageHandler channelHandler;

    public volatile boolean closedByPeer;
    private volatile boolean closed;

    public EventContext(SocketChannel socketChannel, SelectionKey selectionKey, MessageHandler channelHandler) {
        this.channel = socketChannel;
        this.selectionKey = selectionKey;
        this.channelHandler = channelHandler;

        init();
    }

    private void init() {
        decodeInvocation = Invocation.buildInvocationChain(
                this, channelHandler.decodePipeline(),
                (msg) -> channelHandler.onMessage(this, msg));

        encodeInvocation = Invocation.buildInvocationChain(
                this, channelHandler.encodePipeline(),
                (msg) -> {
                    if (msg instanceof ByteBuffer buf)
                        write0(buf);
                    else
                        throw new ClassCastException("Unsupported type:" + msg.getClass());
                });
    }

    public ListenableFutureTask write(Object msg) {
        encodeInvocation.invoke(msg);
        return null;
    }

    /**
     * 1. 返回 Promise, 写缓冲中每个 ByteBuf 都可以关联一个 listener, 在实际写入 socket 后调用.
     * 2. 写缓冲应该由编码 pipeline 维护(待定). receiver 的逻辑就是 put 到 buffer
     *
     * todo 1待实现 2已实现
     */
    private ListenableFutureTask write0(ByteBuffer buf) {
        synchronized (buffersToWrite) {
            if (closed) {
                throw new RuntimeException("Connection closed");
            }
            buffersToWrite.offer(buf);
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
        return null;
    }

    //todo移到 event handler里处理
    public int read(ByteBuffer buf) throws IOException {
        //The number of bytes read, possibly zero, or -1 if the channel has reached end-of-stream
        int n = channel.read(buf);
        if (n == -1) {
            log.debug("Received FIN from {}", channel.getRemoteAddress());
            closedByPeer = true;
            //能否关 socket 取决于写缓冲中是否还有未写完的数据, 如果没有,则可直接关闭; 如果没有,则要在写完的时候关闭
            synchronized (buffersToWrite) {
                if (buffersToWrite.isEmpty()) {
                    channel.close();
                    selectionKey.cancel();
                    closed = true;
                }
            }
        }
        return n;
    }

    public void close() throws IOException {
        closed = true;
        channel.close();
        selectionKey.cancel();
    }
}
