package io.github.light0x00.letty.old;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;

/**
 * @author lightx00
 * @since 2023/6/16
 */
@Slf4j
public class MainReactor extends Reactor implements Runnable {
    private final SocketAddress address;
    private final EventHandler handler;

    private SubReactor[] reactors;

    public MainReactor(SocketAddress address, EventHandler handler) {
        this.address = address;
        this.handler = handler;
    }

    private SubReactor assignReactor(SocketChannel sc) {
        int hash = Objects.hashCode(sc);
        int index = hash & (reactors.length - 1);
        return reactors[index];
    }

    @SneakyThrows
    @Override
    public void run() {
        ServerSocketChannel ssc = ServerSocketChannel.open(StandardProtocolFamily.INET);
        ssc.bind(address);
        ssc.configureBlocking(false);

        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        log.debug("Listen on {}", address);

        try {
            eventLoop(selector, this::handleEvent);
        }finally {
            selector.close();
            ssc.close();
        }
    }

    @SneakyThrows
    protected void handleEvent(SelectionKey event) {
        if (event.isAcceptable()) {
            SocketChannel channel = ((ServerSocketChannel) event.channel()).accept();
            /*
             * Register to selector, note that for a server-side connection.
             * "acceptable event" refers to a connection established,
             * while for client-side, "connectable event" carries the same meaning.
             * */
            SubReactor subReactor = assignReactor(channel);
            subReactor.register(channel);
        }
    }

}
