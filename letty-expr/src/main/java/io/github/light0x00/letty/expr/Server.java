package io.github.light0x00.letty.expr;


import io.github.light0x00.letty.expr.eventloop.NioEventLoop;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.expr.handler.ChannelHandlerConfigurer;
import io.github.light0x00.letty.expr.handler.EventHandler;
import io.github.light0x00.letty.expr.handler.IOEventHandler;
import io.github.light0x00.letty.expr.handler.ServerIOEventHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/6/28
 */
@Slf4j
public class Server {

    final NioEventLoopGroup parent;

    final NioEventLoopGroup child;

    final ChannelHandlerConfigurer channelHandlerConfigurer;

    final Acceptor acceptor;

    public Server(NioEventLoopGroup group, ChannelHandlerConfigurer messageHandler) {
        this(group, group, messageHandler);
    }

    public Server(NioEventLoopGroup parent, NioEventLoopGroup child, ChannelHandlerConfigurer channelHandlerConfigurer) {
        this.parent = parent;
        this.child = child;
        this.channelHandlerConfigurer = channelHandlerConfigurer;

        acceptor = new Acceptor(child, channelHandlerConfigurer);
    }

    @SneakyThrows
    public ListenableFutureTask<Void> bind(SocketAddress address) {
        ServerSocketChannel ssc = ServerSocketChannel.open(StandardProtocolFamily.INET);
        ssc.configureBlocking(false);
        ssc.bind(address);

        var bindFuture = new ListenableFutureTask<Void>(null);

        parent.next().register(ssc, SelectionKey.OP_ACCEPT, acceptor)
                .addListener((f) -> {
                    log.debug("Listen on " + address);
                    bindFuture.run();
                });
        return bindFuture;
    }

    public void shutdown() {

    }

    private record Acceptor(NioEventLoopGroup group,
                            ChannelHandlerConfigurer channelHandlerConfigurer) implements EventHandler {

        @SneakyThrows
        @Override
        public void onEvent(SelectionKey key) {
            SocketChannel incomingChannel = ((ServerSocketChannel) key.channel()).accept();
            incomingChannel.configureBlocking(false);
            NioEventLoop eventLoop = group.next();
            eventLoop.register(incomingChannel, SelectionKey.OP_READ)
                    .addListener(futureTask -> {
                        SelectionKey selectionKey = futureTask.get();
                        IOEventHandler handler = new ServerIOEventHandler(eventLoop, incomingChannel, selectionKey, channelHandlerConfigurer);
                        selectionKey.attach(handler);
                    }, eventLoop);

        }
    }

}
