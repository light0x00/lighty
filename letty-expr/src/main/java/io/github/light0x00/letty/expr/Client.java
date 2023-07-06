package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.eventloop.NioEventLoop;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.expr.handler.ChannelHandlerConfigurer;
import io.github.light0x00.letty.expr.handler.IOEventHandler;
import lombok.SneakyThrows;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/6/29
 */
public class Client {

    final NioEventLoopGroup group;

    ChannelHandlerConfigurer handlerConfigurer;

    public Client(NioEventLoopGroup group, ChannelHandlerConfigurer handlerConfigurer) {
        this.group = group;
        this.handlerConfigurer = handlerConfigurer;
    }

    @SneakyThrows
    public ListenableFutureTask<Void> connect(SocketAddress address) {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        var connectedFuture = new ListenableFutureTask<Void>(null);
        NioEventLoop eventLoop = group.next();
        eventLoop.register(channel, SelectionKey.OP_CONNECT)
                .addListener(new FutureListener<SelectionKey>() {
                    @SneakyThrows
                    @Override
                    public void operationComplete(ListenableFutureTask<SelectionKey> f) {
                        SelectionKey key = f.get();
                        // attach context before connect , so that to ensure the context not null when the event triggered.
                        // connect 动作应发生在 attach context 之后, 这样才能保证事件触发时能拿到非空的 context
                        IOEventHandler eventHandler = new IOEventHandler(eventLoop, channel, key, handlerConfigurer);
                        key.attach(eventHandler);

                        channel.connect(address);

                        eventHandler.connectedFuture()
                                .addListener((f2) -> connectedFuture.run());
                    }
                });
        return connectedFuture;
    }

    public void shutdown() {

    }
}
