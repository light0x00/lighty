package io.github.light0x00.letty.expr.eventloop;

import io.github.light0x00.letty.expr.EventHandler;
import io.github.light0x00.letty.expr.ListenableFutureTask;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
public class NioEventLoop implements EventExecutor {

    private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

    private final Selector selector = Selector.open();

    private final Executor executor;

    private final AtomicBoolean started = new AtomicBoolean();

    private volatile Thread workerThread;

//    private final ChannelHandler channelHandler;

    public NioEventLoop(Executor executor) throws IOException {
//        this.channelHandler = channelHandler;
        this.executor = executor;
    }

    public void addTask(Runnable runnable, boolean wakeup) {
        tasks.offer(runnable);
        if (wakeup) {
            wakeup();
        }
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        addTask(command, true);
        if (!inEventLoop()) {
            startup();
        }
    }

    public ListenableFutureTask<SelectionKey> register(final SelectableChannel channel, final int interestOps) {
        return register(channel, interestOps, null);
    }

    /**
     * Register a channel to the underlying selector with attachment.
     *
     * @implNote The listeners of the future returned, will be executed in event loop by default.
     * Be aware the concurrent race when change the executor.
     */
    public ListenableFutureTask<SelectionKey> register(final SelectableChannel channel, final int interestOps, @Nullable Object att) {
        var future = new ListenableFutureTask<>(new Callable<SelectionKey>() {
            @Override
            @SneakyThrows
            public SelectionKey call() {
                return channel.register(selector, interestOps, att);
            }
        }, this);
        execute(future);
        return future;
    }

    public void wakeup() {
        selector.wakeup();
    }

    private void startup() {
        if (!started.get() && started.compareAndSet(false, true)) {
            executor.execute(this::run);
        }
    }

    public void shutdown() {
        if (started.get()) {
            while (workerThread == null) {
                Thread.onSpinWait();
            }
            workerThread.interrupt();
        }
    }

    @SneakyThrows
    private void run() {
        workerThread = Thread.currentThread();
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
                var channelHandler = (EventHandler) event.attachment();
                channelHandler.onEvent(event);
                it.remove();
            }
        }
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == workerThread;
    }
}
