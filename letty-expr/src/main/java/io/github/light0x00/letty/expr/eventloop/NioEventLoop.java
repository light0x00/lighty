package io.github.light0x00.letty.expr.eventloop;

import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.handler.EventHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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
import java.util.function.Function;

/**
 * @author light0x00
 * @since 2023/6/16
 */
@Slf4j
public class NioEventLoop implements EventExecutor {

    private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

    private final Selector selector = Selector.open();

    private final Executor executor;

    private final AtomicBoolean started = new AtomicBoolean();

    private volatile Thread workerThread;

    @Getter
    private EventExecutorGroup<NioEventLoop> group;

    public NioEventLoop(Executor executor, EventExecutorGroup<NioEventLoop> group) throws IOException {
        this.executor = executor;
        this.group = group;
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

    public ListenableFutureTask<SelectionKey> register(SelectableChannel channel, int interestOps, @Nullable EventHandler handler) {
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
                safeExecute(c);
            }
            selector.select();
            Set<SelectionKey> events = selector.selectedKeys();
            Iterator<SelectionKey> it = events.iterator();
            while (it.hasNext()) {
                SelectionKey event = it.next();
                var eventHandler = (EventHandler) event.attachment();
                try {
                    eventHandler.onEvent(event);
                } catch (Throwable th) {
                    log.error("Error occurred while process event", th); //TODO 交给异常捕获
                    //socket interestSet readySet 都是有状态的, 原则上只要处理时出现未处理异常,就应当 fail-fast, 避免基于错误的状态继续.
                    event.cancel();
                    event.channel().close();
                }
                it.remove();
            }
        }
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
