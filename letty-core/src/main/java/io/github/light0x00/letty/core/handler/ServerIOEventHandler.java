package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.LettyConfiguration;
import io.github.light0x00.letty.core.eventloop.NioEventLoop;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public class ServerIOEventHandler extends IOEventHandler {
    public ServerIOEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, LettyConfiguration configurer) {
        super(eventLoop, channel, key, configurer);

        eventLoop.execute(this::processAcceptableEvent);    //在后续的事件循环中执行, 避免初始化阶段执行用户代码.
    }

    private void processAcceptableEvent() {
        connectableFuture.setSuccess(channel);
        eventNotifier.onConnected(context);
    }
}
