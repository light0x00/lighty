package io.github.light0x00.letty.core;

import lombok.SneakyThrows;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author lightx00
 * @since 2023/6/16
 */
public class SubReactor extends Reactor implements Runnable {

    private final EventHandler handler;

    protected Selector selector;

    @SneakyThrows
    public SubReactor(EventHandler handler) {
        this.handler = handler;
        selector = Selector.open();
    }

    public void register(SocketChannel socketChannel) {
        addTask(
                new Runnable() {
                    @Override
                    @SneakyThrows
                    public void run() {
                        socketChannel.configureBlocking(false);
                        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                        handler.onAcceptable(socketChannel, key);
                    }
                }
        );
        selector.wakeup();
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            eventLoop(selector, this::handleEvent);
        } finally {
            selector.close();
        }
    }

    @SneakyThrows
    protected void handleEvent(SelectionKey event) {
        if (event.isReadable()) {
            SocketChannel channel = (SocketChannel) event.channel();
            handler.onReadable(channel, event);
        } else if (event.isWritable()) {
            SocketChannel channel = (SocketChannel) event.channel();
            handler.onWriteable(channel, event);
        }
    }
}
