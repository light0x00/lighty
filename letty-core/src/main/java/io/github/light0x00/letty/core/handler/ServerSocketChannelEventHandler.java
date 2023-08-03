package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.LettyConfiguration;
import io.github.light0x00.letty.core.eventloop.NioEventLoop;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public class ServerSocketChannelEventHandler extends SocketChannelEventHandler {
    public ServerSocketChannelEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, LettyConfiguration configurer) {
        super(eventLoop, channel, key, configurer);

        eventLoop.execute(this::processAcceptableEvent);    //在后续的事件循环中执行, 避免初始化阶段执行用户代码.
    }

    private void processAcceptableEvent() {
        dispatcher.onConnected(context);
        connectableFuture.setSuccess(channel);
    }
}
