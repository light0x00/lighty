package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.eventloop.NioEventLoop;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.expr.handler.ChannelHandlerConfigurer;
import io.github.light0x00.letty.expr.handler.EventHandler;
import io.github.light0x00.letty.expr.handler.ServerIOEventHandler;
import lombok.SneakyThrows;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/7/7
 */
public record Acceptor(NioEventLoopGroup group,
                       ChannelHandlerConfigurer channelHandlerConfigurer) implements EventHandler {

    @SneakyThrows
    @Override
    public void onEvent(SelectionKey key) {
        SocketChannel incomingChannel = ((ServerSocketChannel) key.channel()).accept();
        incomingChannel.configureBlocking(false);
        NioEventLoop eventLoop = group.next();

        eventLoop.register(incomingChannel, SelectionKey.OP_READ, (selectionKey) -> {
            return new ServerIOEventHandler(eventLoop, incomingChannel, selectionKey, channelHandlerConfigurer);
        });
    }
}
