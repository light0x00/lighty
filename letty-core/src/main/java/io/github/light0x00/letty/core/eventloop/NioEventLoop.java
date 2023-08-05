package io.github.light0x00.letty.core.eventloop;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.concurrent.RejectedExecutionHandler;
import io.github.light0x00.letty.core.handler.NioEventHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.github.light0x00.letty.core.util.Tool.slf4jFormat;

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
    private final RejectedExecutionHandler rejectedExecutionHandler = (task, executor) -> {
        throw new RejectedExecutionException();
    };

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
            addTask(command, true);
            if (state.get() != RUNNING) {
                if (tasks.remove(command)) {
                    rejectedExecutionHandler.rejected(command, this);
                }
            }
        } else {
            rejectedExecutionHandler.rejected(command, this);
        }

    }

    public ListenableFutureTask<SelectionKey> register(SelectableChannel channel, int interestOps, NioEventHandler handler) {
        return register(channel, interestOps, (k) -> handler);
    }

    /**
     * Register a channel to the underlying selector with attachment.
     *
     * @implNote The listeners of the future returned, will be executed in event loop by default.
     * Be aware the concurrent race when change the executor.
     */
    public ListenableFutureTask<SelectionKey> register(SelectableChannel channel,
                                                       int interestOps,
                                                       Function<SelectionKey, NioEventHandler> eventHandlerProvider) {
        var registerFuture = new ListenableFutureTask<>(new Callable<SelectionKey>() {
            @Override
            @SneakyThrows
            public SelectionKey call() {
                if (!channel.isOpen()) {
                    //Indicate the channel closing operation happened before current register operation.
                    //Usually it happened in the previous event loop.
                    //For this situation, a ClosedChannelException will be thrown.
                    log.warn("Channel has been closed");
                }
                SelectionKey key = channel.register(selector, interestOps);
                try {
                    key.attach(eventHandlerProvider.apply(key));
                } catch (Throwable th) {
                    key.cancel();   //避免发生注册成功, 但是 attachment 为空的情况
                    throw th;
                }
                return key;
            }
        }, this);
        if (inEventLoop()) {
            registerFuture.run();
        } else {
            execute(registerFuture);
        }
        return registerFuture;
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
            Runnable r;
            while ((r = tasks.poll()) != null) {
                try {
                    r.run();
                    processResultIfPossible(r);
                } catch (Throwable th) {
                    log.warn("Error occurred while process task", th);
                }
            }
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
        onTerminated();
        log.debug("Event loop terminated");
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
        log.debug("All the resource associated with event loop released");

        shutdownFuture.setSuccess();
    }

    private static void invokeClose(NioEventHandler handler) {
        try {
            handler.close();
        } catch (Throwable t) {
            log.warn(slf4jFormat("An exception was thrown by handler's close: {}", handler), t);
        }
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == workerThread;
    }
}
