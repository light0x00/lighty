package io.github.light0x00.letty.expr.light0x00.letty.core;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * @author light0x00
 * @since 2023/6/19
 */
public class Test {
    /*
    1. 水平触发,事件不处理,每次 select 都会返回事件
    2.
    * */
    static class Server {

        @SneakyThrows
        public static void main(String[] args) {
            ServerSocketChannel ssc = ServerSocketChannel.open(StandardProtocolFamily.INET);
            ssc.bind(new InetSocketAddress("0.0.0.0", 9000));
            ssc.configureBlocking(false);

            Selector selector = Selector.open();
            SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                int select = selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {

                    if (selectionKey.isAcceptable()) {

                    }

                }
                selectionKeys.clear();
            }
        }

    }

    static class Client {
        @SneakyThrows
        public static void main(String[] args) {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(new InetSocketAddress("0.0.0.0", 9000));
        }
    }
}
