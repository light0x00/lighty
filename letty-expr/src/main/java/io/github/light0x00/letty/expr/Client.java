package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
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

    ChannelHandlerConfigurer messageHandler;

    public Client(NioEventLoopGroup group, ChannelHandlerConfigurer messageHandler) {
        this.group = group;
        this.messageHandler = messageHandler;
    }

    @SneakyThrows
    public ListenableFutureTask<Void> connect(SocketAddress address) {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);

        var connectFuture = new ListenableFutureTask<Void>(null);

        group.next()
                .register(channel, SelectionKey.OP_CONNECT)
                .addListener(new FutureListener<SelectionKey>() {
                    @SneakyThrows
                    @Override
                    public void operationComplete(ListenableFutureTask<SelectionKey> f) {
                        SelectionKey key = f.get();
                        // attach context before connect , so that to ensure the context not null when the event triggered.
                        // connect 动作应发生在 attach context 之后, 这样才能保证事件触发时能拿到非空的 context
                        key.attach(new ChannelIOHandler(channel, key, messageHandler));

                        channel.connect(address);

                        connectFuture.run();
                    }
                });
        return connectFuture;
    }

    public void shutdown() {

    }
}
