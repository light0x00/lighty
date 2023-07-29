package io.github.light0x00.letty.old.expr;

import io.github.light0x00.letty.old.concurrent.ListenableFutureTask;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author light0x00
 * @since 2023/6/16
 */
public class EventLoop {

    private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

    private final Selector selector = Selector.open();

    private final Executor executor;

    private final AtomicBoolean started = new AtomicBoolean();

    private volatile Thread thread;

    public EventLoop(Executor executor) throws IOException {
        this.executor = executor;
    }

    public void addTask(Runnable runnable) {
        tasks.offer(runnable);
    }

    public ListenableFutureTask<SelectionKey> register(final SelectableChannel channel, final int interestOps, Object atta) {
        /*
         * 这里会执行两次 cas ,一次是当前 event loop, 一次是被后的线程
         *
         * netty 采用继承
         */
        startup();
        var future = new ListenableFutureTask<>(new Callable<SelectionKey>() {
            @Override
            @SneakyThrows
            public SelectionKey call() {
                channel.configureBlocking(false);
                SelectionKey key = channel.register(selector, interestOps);
                return key;
            }
        });
        addTask(future);
        return future;
    }

    public void wakeup() {
        selector.wakeup();
    }

    public void startup() {
        if (!started.get() && started.compareAndSet(false, true)) {
            executor.execute(this::run);
        }
    }

    public void shutdown() {
        if (started.get()) {
            while (thread == null) {
                Thread.onSpinWait();
            }
                thread.interrupt();
        }
    }

    @SneakyThrows
    private void run() {
        thread = Thread.currentThread();
        while (!Thread.currentThread().isInterrupted()) {
            Runnable c;
            while ((c = tasks.poll()) != null) {
                c.run();
            }
            selector.select();
            Set<SelectionKey> events = selector.selectedKeys();
            Iterator<SelectionKey> it = events.iterator();
            while (it.hasNext()) {
                SelectionKey event = it.next();
                //参考 netty NioEventLoop#processSelectedKeysPlain 682
//                handler.accept(event);
                it.remove();
            }
        }
    }
}
