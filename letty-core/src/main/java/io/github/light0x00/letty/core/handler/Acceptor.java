package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.LettyConfiguration;
import io.github.light0x00.letty.core.eventloop.NioEventLoop;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author light0x00
 * @since 2023/7/7
 */
@Slf4j
public class Acceptor implements NioEventHandler {

    private SelectionKey key;
    private ServerSocketChannel channel;
    private NioEventLoopGroup workerGroup;
    private LettyConfiguration lettyConfiguration;

    public Acceptor(ServerSocketChannel channel,
                    SelectionKey key,
                    NioEventLoopGroup workerGroup, LettyConfiguration lettyConfiguration) {
        this.channel = channel;
        this.key = key;
        this.workerGroup = workerGroup;
        this.lettyConfiguration = lettyConfiguration;
    }

    @SneakyThrows
    @Override
    public void onEvent(SelectionKey key) {
        SocketChannel incomingChannel = ((ServerSocketChannel) key.channel()).accept();
        incomingChannel.configureBlocking(false);
        NioEventLoop eventLoop = workerGroup.next();

        eventLoop.register(incomingChannel, SelectionKey.OP_READ, (selectionKey) ->
                new SocketChannelEventHandler(eventLoop, incomingChannel, selectionKey, lettyConfiguration) {
                    {
                        dispatcher.onConnected();
                        connectableFuture.setSuccess(channel);
                    }
                }
        );
    }

    @SneakyThrows
    @Override
    public void close() {
        String name = channel.toString();
        channel.close();
        log.debug("Release resource associated with channel {}", name);
    }
}
