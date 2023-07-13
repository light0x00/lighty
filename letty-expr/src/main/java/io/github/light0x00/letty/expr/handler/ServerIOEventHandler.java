package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.ChannelConfigurationProvider;
import io.github.light0x00.letty.expr.eventloop.NioEventLoop;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public class ServerIOEventHandler extends IOEventHandler {
    public ServerIOEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, ChannelConfigurationProvider configurer) {
        super(eventLoop, channel, key, configurer);

        processAcceptableEvent();
    }

    private void processAcceptableEvent() {
        eventNotifier.onConnected(context);
        connectedFuture.run();
    }
}
