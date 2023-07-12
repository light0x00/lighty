package io.github.light0x00.letty.expr.eventloop;

import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.handler.EventHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author light0x00
 * @since 2023/6/16
 */
@Slf4j
public class NioEventLoop implements EventLoop {

    private static final int NOT_STARTED = 0;
    private static final int STARTED = 1;
    private static final int TERMINATED = 2;

    private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

    private final Selector selector;

    private final Executor executor;

    private final AtomicInteger state = new AtomicInteger();

    private volatile Thread workerThread;

    @Getter
    private final EventLoopGroup<NioEventLoop> group;

    @Getter
    private final ListenableFutureTask<Void> shutdownFuture = new ListenableFutureTask<>(null);

    @SneakyThrows
    public NioEventLoop(Executor executor, EventLoopGroup<NioEventLoop> group) {
        this.executor = executor;
        this.group = group;
        selector = Selector.open();
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

    public ListenableFutureTask<SelectionKey> register(SelectableChannel channel, int interestOps, EventHandler handler) {
        return register(channel, interestOps, (k) -> handler);
    }

    /**
     * Register a channel to the underlying selector with attachment.
     *
     * @implNote The listeners of the future returned, will be executed in event loop by default.
     * Be aware the concurrent race when change the executor.
     */
    public ListenableFutureTask<SelectionKey> register(SelectableChannel channel, int interestOps, Function<SelectionKey, EventHandler> eventHandlerProvider) {
        var future = new ListenableFutureTask<>(new Callable<SelectionKey>() {
            @Override
            @SneakyThrows
            public SelectionKey call() {
                SelectionKey key = channel.register(selector, interestOps);
                key.attach(eventHandlerProvider.apply(key));
                return key;
            }
        }, this);
        execute(future);
        return future;
    }

    public void wakeup() {
        selector.wakeup();
    }

    private void startup() {
        if (state.compareAndSet(NOT_STARTED, STARTED)) {
            executor.execute(this::run);
        }
    }

    @Override
    public ListenableFutureTask<Void> shutdown() {
        if (state.compareAndSet(NOT_STARTED, TERMINATED)) {
            onTerminated();
        } else if (state.compareAndSet(STARTED, TERMINATED)) {
            //状态为 started 和 worker 开始运行之间存在时间差,
            //所以这里短暂自旋,等待 workerThread 被赋值
            while (workerThread == null) {
                Thread.onSpinWait();
            }
            workerThread.interrupt();
        } else {
            //这种情况说明已经被 shutdown 了
        }
        return shutdownFuture;
    }

    @SneakyThrows
    private void run() {
        workerThread = Thread.currentThread();
        while (!Thread.currentThread().isInterrupted()) {
            Runnable c;
            while ((c = tasks.poll()) != null) {
                safeExecute(c);
            }
            selector.select();
            Set<SelectionKey> events = selector.selectedKeys();
            Iterator<SelectionKey> it = events.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                var eventHandler = (EventHandler) key.attachment();
                try {
                    eventHandler.onEvent(key);
                } catch (Throwable th) {
                    log.error("Error occurred while process event", th); //TODO 交给异常捕获
                    if (th instanceof IOException) {
                        key.cancel();
                        key.channel().close();
                    }
                }
                it.remove();
            }
        }
        onTerminated();
    }

    @SneakyThrows
    private void onTerminated() {
        selector.close();
        shutdownFuture.run();
    }

    private void safeExecute(Runnable r) {
        try {
            r.run();
        } catch (Throwable th) {
            log.error("", th);
            //TODO 交给异常捕获
        }
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == workerThread;
    }
}
