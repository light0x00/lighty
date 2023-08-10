package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoop;
import io.github.light0x00.lighty.core.facade.*;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/8/10
 */
public class SocketChannelEventHandlerS extends SocketChannelEventHandler {

    public SocketChannelEventHandlerS(NioEventLoop eventLoop, SocketChannel javaChannel, SelectionKey key, ChannelInitializer<InitializingNioSocketChannel> channelInitializer, LightyConfiguration configuration, ListenableFutureTask<NioSocketChannel> connectableFuture) {
        super(eventLoop, javaChannel, key, channelInitializer, configuration, connectableFuture);
        processAcceptableEvent();
    }

    private void processAcceptableEvent() {
        // 对于 server 侧的 SocketChannel 而言, 其 connected 事件, 在 ServerSocketChannel acceptable 时就触发
        this.connectableFuture.setSuccess(this.channel);
        this.dispatcher.onConnected();
    }

    @Override
    public void processEvent(SelectionKey key) {
        throw new LightyException("Unsupported event");
    }
}
