package io.github.light0x00.lighty.core.eventloop;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.concurrent.RejectedExecutionHandler;
import io.github.light0x00.lighty.core.dispatcher.NioEventHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.light0x00.lighty.core.util.Tool.slf4jFormat;

/**
 * @author light0x00
 * @since 2023/6/16
 */
@Slf4j
public class NioEventLoop implements EventExecutor {

    private static final int NOT_STARTED = 0;
    private static final int RUNNING = 1;
    private static final int TERMINATED = 2;

    private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

    /**
     * 如果调用 {@link SingleThreadExecutor#execute(Runnable)} 添加的任务,
     * 因任何原因而导致未被执行,(如队列容量上限或已经shutdown),
     * 都将转交给 {@link RejectedExecutionHandler}
     */
    private final RejectedExecutionHandler reachLimitCauseRejectedExecutionHandler = (task, executor) -> {
        throw new RejectedExecutionException("Rejected execution, the pending tasks reach the maximum number.");
    };

    private final RejectedExecutionHandler shutdownCauseRejectedExecutionHandler = (task, executor) -> {
        throw new RejectedExecutionException("Rejected execution, event-loop already shutdown!");
    };

    private final Selector selector;

    private final Executor executor;

    private final AtomicInteger state = new AtomicInteger();

    private volatile Thread workerThread;

    @Getter
    private final EventExecutorGroup<NioEventLoop> group;

    @Getter
    private final ListenableFutureTask<Void> shutdownFuture = new ListenableFutureTask<>(null);

    @SneakyThrows
    public NioEventLoop(Executor executor, EventExecutorGroup<NioEventLoop> group) {
        this.executor = executor;
        this.group = group;
        selector = Selector.open();
    }

    @Override
    public void execute(@Nonnull Runnable command) {

        if (state.get() == NOT_STARTED && state.compareAndSet(NOT_STARTED, RUNNING)) {
            executor.execute(this::run);
        }
        /*
         * The race condition:
         * - shutdown,state = SHUTDOWN
         * - execute, if state == STARTED then do
         *
         * Inspired from `ThreadPoolExecutor#execute` written by the Doug Lea.
         * Here we use lock-free way to solve the race condition. It consists of three phases:
         * 1.check condition
         * 2.do
         * 3.recheck condition, then decide confirm or rollback
         * */
        if (state.get() == RUNNING) {
            if (!tasks.offer(command)) {
                reachLimitCauseRejectedExecutionHandler.rejected(command, this);
            }
            if (state.get() != RUNNING) {
                if (tasks.remove(command)) {
                    shutdownCauseRejectedExecutionHandler.rejected(command, this);
                }
            } else {
                wakeup();
            }
        } else {
            shutdownCauseRejectedExecutionHandler.rejected(command, this);
        }

    }

    /**
     * Register a channel to the underlying selector with attachment.
     *
     * @implNote The listeners of the future returned, will be executed in event loop by default.
     * Be aware the concurrent race when change the executor.
     */
    public <T extends NioEventHandler> ListenableFutureTask<T> register(SelectableChannel channel,
                                                                        int interestOps,
                                                                        EventHandlerProvider<T> eventHandlerProvider) {
        if (inEventLoop()) {
            register0(channel, interestOps, eventHandlerProvider);
            return ListenableFutureTask.successFuture();
        } else {
            return submit(() -> register0(channel, interestOps, eventHandlerProvider));
        }
    }

    @SneakyThrows
    @Nonnull
    private <T extends NioEventHandler> T register0(SelectableChannel channel, int interestOps, EventHandlerProvider<T> eventHandlerProvider) {
        SelectionKey key = channel.register(selector, interestOps);
        try {
            T eventHandler = Objects.requireNonNull(eventHandlerProvider.get(key));
            key.attach(eventHandler);
            log.debug("Register channel({}) on selector with interest {}", channel, interestOps);
            return eventHandler;
        } catch (Throwable th) {
            //必须确保 Selector 中每一个注册的 SelectionKey 的 attachment 都为 EventHandler
            //如果获取 eventHandler 异常, 则必须取消掉 SelectionKey, 避免后续取用时 NPE
            key.cancel();
            throw th;
        }
    }

    public void wakeup() {
        selector.wakeup();
    }

    @Override
    public ListenableFutureTask<Void> shutdown() {
        if (state.compareAndSet(NOT_STARTED, TERMINATED)) {
            shutdownFuture.setSuccess();
        } else if (state.compareAndSet(RUNNING, TERMINATED)) {
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
        log.debug("Event loop started");
        workerThread = Thread.currentThread();
        while (!Thread.currentThread().isInterrupted()) {
            processAllTasks();
            selector.select();
            Set<SelectionKey> events = selector.selectedKeys();
            Iterator<SelectionKey> it = events.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                var eventHandler = (NioEventHandler) key.attachment();
                try {
                    eventHandler.onEvent(key);
                } catch (Throwable th) {
                    //这种异常正常只会来源于底层的错误, 属于不可预测的错误, 比如 Connect Refused.
                    log.error("Error occurred while process event", th);
                    invokeClose(eventHandler);
                }
                it.remove();
            }
        }
        //Always ensure all the tasks executed, even through shutdown
        //It means there is no `shutdownNow`, which drop the remaining tasks in queue.
        processAllTasks();
        onTerminated();
        log.debug("Event loop terminated");
    }

    private void processAllTasks() {
        Runnable r;
        while ((r = tasks.poll()) != null) {
            try {
                r.run();
                processResultIfPossible(r);
            } catch (Throwable th) {
                log.debug("Error occurred while process task", th);
            }
        }
    }

    private static void processResultIfPossible(Runnable r) throws Throwable {
        if (r instanceof ListenableFutureTask<?> future) {
            if (future.cause() != null) {
                throw future.cause();
            }
        }
    }

    @SneakyThrows
    private void onTerminated() {
        for (SelectionKey key : selector.keys()) {
            var handler = (NioEventHandler) key.attachment();
            assert handler != null;
            invokeClose(handler);
        }
        selector.close();
        log.debug("Released all the resources associated with event-loop {}", this);

        shutdownFuture.setSuccess();
    }

    private static void invokeClose(NioEventHandler handler) {
        try {
            handler.onEventLoopShutdown();
        } catch (Throwable t) {
            log.warn(slf4jFormat("An exception was thrown by handler's close: {}", handler), t);
        }
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == workerThread;
    }
}
