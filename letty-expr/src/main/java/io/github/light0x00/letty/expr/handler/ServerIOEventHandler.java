package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.eventloop.NioEventLoop;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 在处理 I/O 事件方面, Server 端与 Client 端的不同之处在于,
 * 前者的连接建立事件是 ServerSocketChannel 的 Acceptable 事件, 可能在与 SocketChannel 不同的 EventLoop
 * 后者则是 SocketChannel 的 Connectable 事件
 *
 * @author light0x00
 * @since 2023/7/4
 */
public class ServerIOEventHandler extends IOEventHandler{
    public ServerIOEventHandler(NioEventLoop eventLoop, SocketChannel channel, SelectionKey key, ChannelHandlerConfigurer configurer) {
        super(eventLoop, channel, key, configurer);
        connectedNotifier.run();
    }
}
