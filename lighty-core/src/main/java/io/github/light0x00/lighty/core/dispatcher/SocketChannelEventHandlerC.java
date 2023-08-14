package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.buffer.BufferPool;
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoop;
import io.github.light0x00.lighty.core.facade.*;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/8/10
 */
public class SocketChannelEventHandlerC extends SocketChannelEventHandler {

    public SocketChannelEventHandlerC(NioEventLoop eventLoop, SocketChannel javaChannel, SelectionKey key, ChannelInitializer<InitializingNioSocketChannel> channelInitializer, LightyProperties lettyProperties, BufferPool bufferPool, ListenableFutureTask<NioSocketChannel> connectableFuture) {
        super(eventLoop, javaChannel, key, channelInitializer, lettyProperties, bufferPool, connectableFuture);
    }

    @Override
    public void processEvent(SelectionKey key) {
        processConnectableEvent();
    }

    private void processConnectableEvent() {
        try {
            javaChannel.finishConnect();
        } catch (IOException e) {
            connectableFuture.setFailure(e);
            destroy();
            return;
        }
        connectableFuture.setSuccess(channel);
        dispatcher.onConnected();
        key.interestOps((key.interestOps() ^ SelectionKey.OP_CONNECT) | SelectionKey.OP_READ);
    }

    @SneakyThrows
    public ListenableFutureTask<NioSocketChannel> connect(SocketAddress address) {
        if (eventLoop.inEventLoop()) {
            connect0(address);
        } else {
            eventLoop.submit(() -> connect0(address));
        }
        return connectableFuture;
    }

    @SneakyThrows
    private void connect0(SocketAddress address) {
        try {
            javaChannel.connect(address);
        } catch (Throwable t) {
            connectableFuture.setFailure(t);
            close();
            throw t;
        }
    }

}
