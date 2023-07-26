package io.github.light0x00.letty.expr.light0x00.letty.core;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author light0x00
 * @since 2023/6/19
 */
@Slf4j
public class Test {
    /*

     * */
    static class Server {

        @SneakyThrows
        public static void main(String[] args) {
            ServerSocketChannel ssc = ServerSocketChannel.open(StandardProtocolFamily.INET);
            ssc.bind(new InetSocketAddress("0.0.0.0", 9000));
            ssc.configureBlocking(false);

            EventLoop eventLoop = new EventLoop();

            AtomicInteger connCuunt = new AtomicInteger();
            eventLoop.register(ssc, SelectionKey.OP_ACCEPT, new EventHandler() {

                @SneakyThrows
                @Override
                public void onEvent(SelectionKey key) {
                    if (key.isAcceptable()) {
                        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
//                        sc.shutdownOutput();
                        eventLoop.register(sc, SelectionKey.OP_READ, this);

                        sc.write(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));

                        if (connCuunt.incrementAndGet() == 2) {
                            eventLoop.shutdown();
                        }
//                        sc.close();
                    } else if (key.isReadable()) {
                        ByteBuffer buf = ByteBuffer.allocate(16);
                        int n = ((SocketChannel) key.channel()).read(buf);
                        if (n == -1) {
                            log.info("Input closed");
                            System.out.println(key.isValid());
                            System.out.println(((SocketChannel) key.channel()).isConnected());
                            System.out.println(key.channel().isOpen());
                            System.out.println(((SocketChannel) key.channel()).socket().isInputShutdown());

                            if (((SocketChannel) key.channel()).socket().isOutputShutdown()) {
                                key.cancel();
                                key.channel().close();
                            } else {
                                key.interestOps(key.interestOps() ^ SelectionKey.OP_READ); //remove read from interestOps
                            }
                        } else {
                            String str = StandardCharsets.UTF_8.decode(buf).toString();
                            log.info(str);
                        }
                    }
                }

            });
        }

    }

    static class Client {
        public static void main(String[] args) throws IOException {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(new InetSocketAddress("0.0.0.0", 9000));

            EventLoop eventLoop = new EventLoop();
            eventLoop.register(sc, SelectionKey.OP_CONNECT, new EventHandler() {
                @SneakyThrows
                @Override
                public void onEvent(SelectionKey key) {
                    if (key.isConnectable()) {
                        ((SocketChannel) key.channel()).finishConnect();
//                        ((SocketChannel) key.channel()).shutdownOutput();

                        key.interestOps(SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        ByteBuffer buf = ByteBuffer.allocate(16);
                        int n = ((SocketChannel) key.channel()).read(buf);
                        if (n == -1) {
                            log.info("Input closed");
                            key.interestOps(key.interestOps() ^ SelectionKey.OP_READ); //remove read from interestOps
                        } else {
                            String str = StandardCharsets.UTF_8.decode(buf.flip()).toString();
                            log.info(str);
                        }
                    }
                }
            });
            System.out.println("!!");
        }
    }


    static class EventLoop implements Executor {

        private static final int NOT_STARTED = 0;
        private static final int STARTED = 1;
        private static final int TERMINATED = 2;

        Selector selector;

        Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

        AtomicInteger state = new AtomicInteger();

        volatile Thread eventLoopThread;

        @SneakyThrows
        EventLoop() {
            selector = Selector.open();
        }

        @SneakyThrows
        private void run() {
            eventLoopThread = Thread.currentThread();
            while (!Thread.currentThread().isInterrupted()) {
                while (!tasks.isEmpty()) {
                    try {
                        tasks.poll().run();
                    } catch (Throwable th) {
                        log.error("", th);
                    }
                }
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys) {
                    try {
                        EventHandler eventHandler = (EventHandler) selectionKey.attachment();
                        eventHandler.onEvent(selectionKey);
                    } catch (Throwable th) {
                        //如果在握手之后,对方通过 RST 中断连接, 那么 java.net.SocketException: Connection reset by peer
                        //此时, selectionKey.isValid() == false && selectionKey.channel().isOpen() == false

                        //如果握手阶段 SYN 收到 RST, 那么 java.net.ConnectException: Connection refused
                        //此时, selectionKey.isValid() == false && selectionKey.channel().isOpen() == false

                        //如果 selectionKey 对应的 channel 被 close, 那么 java.nio.channels.CancelledKeyException: null
                        //此时 selectionKey.isValid() == false && selectionKey.channel().isOpen() == false

                        //如果只调用 selectionKey.cancel() , 那么  java.nio.channels.CancelledKeyException: null
                        ////此时 selectionKey.isValid() == false && selectionKey.channel().isOpen() == true

                        //如果一方先 shutdownInput,另一方 write ,
                        // TCP 层面发送方的数据包会发出去, 但是会收到 RST
                        // java 层面发送方得到: java.net.SocketException: Connection reset
                        // java 层面接收方 read 会返回 -1

                        // 如果一方先 write 数据, 另一方 shutdownInput, TCP 层面数据包会发出去, 且会收到 ACK,
                        // 但是 java 层面接收方 read 会返回 -1 , 即会忽略掉已经 ACK 的数据.

                        //如果一方 shutdownOutput , 那么 TCP 层面会发送 FIN, 而接收方 read 返回 -1
                        //如果一方先 shutdownOutput 后 write, 将得到:  java.nio.channels.AsynchronousCloseException: null
                        log.error("", th);
                        if (th instanceof SocketException) {
                            selectionKey.cancel();
                        }
                    }
                }
                selectionKeys.clear();
            }
            onTerminated();
        }

        @SneakyThrows
        public void register(SelectableChannel channel, int ops, EventHandler att) {
            Runnable runnable = new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    if (!channel.isOpen()) {
                        log.warn("Channel has been closed");
                    }
                    channel.configureBlocking(false);
                    channel.register(selector, ops, att);
                }
            };

            if (inEventLoop()) {
                runnable.run();
            } else {
                execute(runnable);
            }
        }

        @Override
        public void execute(@NotNull Runnable command) {
            if (state.get() == NOT_STARTED && state.compareAndSet(NOT_STARTED, STARTED)) {
                new Thread(this::run).start();
            }

            if (state.get() == STARTED) {
                tasks.add(command);
                selector.wakeup();
            } else {
                throw new RejectedExecutionException();
            }
        }

        public void shutdown() {
            if (state.compareAndSet(NOT_STARTED, TERMINATED)) {
                onTerminated();
            } else if (state.compareAndSet(STARTED, TERMINATED)) {
                //状态为 started 和 worker 开始运行之间存在时间差,
                //所以这里短暂自旋,等待 workerThread 被赋值
                while (eventLoopThread == null) {
                    Thread.onSpinWait();
                }
                eventLoopThread.interrupt();
            } else {
                //这种情况说明已经被 shutdown 了
            }
        }

        public void shutdownGracefully(){

        }

        @SneakyThrows
        private void onTerminated() {
            //TODO 关闭selector 中的 socket
            for (SelectionKey selectionKey : selector.keys()) {
                try {
                    log.info("close:{}", selectionKey.channel());
                    selectionKey.channel().close();
                } catch (Throwable th) {
                    log.error("", th);
                }
            }
            selector.close();
        }


        public boolean inEventLoop() {
            return Thread.currentThread() == eventLoopThread;
        }
    }

    interface EventHandler {
        void onEvent(SelectionKey key) throws IOException;
    }
}
