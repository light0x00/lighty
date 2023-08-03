package io.github.light0x00.letty.core.eventloop;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.concurrent.RejectedExecutionHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author light0x00
 * @since 2023/6/16
 */
@Slf4j
@ThreadSafe
public class SingleThreadExecutor implements EventExecutor {

    private static final int INITIAL = 0;
    private static final int READY = 1;
    private static final int RUNNING = 2;
    private static final int WAITING_STOP = 3;
    private static final int STOP = 4;
    private static final int TERMINATED = 5;

    /**
     * 用于提供 worker 线程
     */
    private final Executor executor;

    /**
     * worker 线程
     */
    private volatile Thread workerThread;

    /**
     * 任务队列
     */
    private final LinkedBlockingQueue<Runnable> queue;

    /**
     * 状态
     * <p>
     * READY:    Not yet started worker.
     * RUNNING:  Accept new tasks and process queued tasks
     * WAITING_STOP: Don't accept new tasks, but process queued tasks
     * STOP:     Don't accept new tasks, don't process queued tasks, and interrupt in-progress tasks
     */
    private volatile int state;

    /**
     * 如果调用 {@link SingleThreadExecutor#execute(Runnable)} 添加的任务,
     * 因任何原因而导致未被执行,(如队列容量上限或已经shutdown),
     * 都将转交给 {@link io.github.light0x00.letty.core.concurrent.RejectedExecutionHandler}
     */
    private final io.github.light0x00.letty.core.concurrent.RejectedExecutionHandler rejectedExecutionHandler;

    private static final io.github.light0x00.letty.core.concurrent.RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = (task, executor) -> {
        throw new RejectedExecutionException();
    };

    private static final AtomicIntegerFieldUpdater<SingleThreadExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadExecutor.class, "state");

    private final ListenableFutureTask<Void> shutdownFuture = new ListenableFutureTask<>(() -> {
    }, null);

    public SingleThreadExecutor(Executor executor) {
        this(Integer.MAX_VALUE, executor);
    }

    public SingleThreadExecutor(Executor executor, io.github.light0x00.letty.core.concurrent.RejectedExecutionHandler rejectedExecutionHandler) {
        this(Integer.MAX_VALUE, executor, rejectedExecutionHandler);
    }

    public SingleThreadExecutor(int maxPendingTasks, Executor executor) {
        this(maxPendingTasks, executor, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public SingleThreadExecutor(int maxPendingTasks, Executor executor, RejectedExecutionHandler rejectedExecutionHandler) {
        this.executor = executor;
        this.queue = new LinkedBlockingQueue<>(maxPendingTasks);
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    /**
     * 执行一个指定任务,如果因任何原因而导致未被执行,比如达到队列容量上限或已经 shutdown/shutdownNow,该任务被转交给 {@link #rejectedExecutionHandler}
     */
    @Override
    public void execute(@Nonnull Runnable command) {
        startWorker();

        /*
         * 如果因为 offer 失败(通常是触发队列容量上限导致), 则直接 reject, 避免 EvenLoop 线程投递任务而阻塞.
         * 参考 netty SingleThreadEventExecutor#addTask
         */
        if (state <= RUNNING && queue.offer(command)) {
            /*
             * 如下两个操作之间的 race condition:
             * execute(Runnable) 会执行一个 if-then-act 的动作, if state == RUNNING then queue.offer(task)
             * shutdown() shutdownNow() 会执行一个写操作 state = WAITING_STOP/STOP
             * 未正确同步时,可能产生如下执行时序:
             * threadA: if state == RUNNING
             * threadB: state = STOP
             * threadA: then queue.offer(task)
             *
             * 这样会导致调用 shutdown 后, 还允许往任务队列里放入任务.
             *
             * 受启发于 ThreadPoolExecutor#execute, 这里通过 “recheck” 来避免 race condition.
             * 即: 先 offer, 然后 recheck state 是否仍旧是 RUNNING, 不是则意味着多放入了任务, 从队列移除任务.
             *
             * 这一套机制其实可能发生如下情况:
             * [一个例子] race condition 带来错误执行时序,导致在非 RUNNING 状态投入任务到队列,并且被 worker 执行
             * 线程A: IF state == RUNNING //复合操作-部分1: 判断条件 (这里假定当前是 RUNNING 状态)
             * 线程B: update state = WAITING_STOP //另一个线程调用 shutdown() 更新了状态
             * 线程A: queue.offer(task) //复合操作-部分2: 放入任务 (此时状态已经改变)
             * 线程C(Worker): state == WAITING_STOP && queue.isEmpty //由于线程 A 放入了任务, 这里将返回 false
             * 线程C: task = queue.poll() //将线程 A 放入的任务取出(并将执行)
             * 线程A: remove(task) // 因为任务已经被 worker 线程C 给取出了, 所以这里无法撤销任务.
             *
             * 但这不会影响什么, 并发环境下这种强一致(较真一个任务是否在 RUNNING 状态下被投入队列)没有太大意义.
             * 当然非要解决,可以引入一个 ReadWriteLock , 控制 if-then-act 复合操作 和 write 操作之间的 race condition.
             */
            if (state > RUNNING) {
                if (queue.remove(command)) {
                    rejectedExecutionHandler.rejected(command, this);
                }
            }
        } else {
            rejectedExecutionHandler.rejected(command, this);
        }
    }

    /**
     * Don't accept new tasks, but process queued tasks
     */
    public ListenableFutureTask<Void> shutdown() {
        shutdown0(false);
        return shutdownFuture;
    }

    /**
     * Don't accept new tasks, don't process queued tasks, and interrupt in-progress tasks
     */
    public ListenableFutureTask<Void> shutdownNow() {
        shutdown0(true);
        return shutdownFuture;
    }

    /**
     * 将状态流转到 {@link #WAITING_STOP } (如果 fore == false) 或 {@link #STOP } (如果 fore == true)
     */
    private void shutdown0(boolean force) {
        /*
         *
         * 状态的流转图如下:
         *
         *     initial────┐
         *        │       │
         *        ▼       │
         * ┌────ready     │
         * │      │       │
         * │      ▼       │
         * │   running    │
         * │      │       │
         * │      ▼       ▼
         * └►waiting_stop/stop
         *
         * 当前操作与如下两个操作存在竞争条件:
         *
         * execute操作: CAS initial -> ready
         * worker操作: CAS ready -> running
         *
         * 前操作(shutdown)会依次尝试如下 3 个 CAS 操作:
         *
         * shutdown线程: CAS initial -> waiting_stop
         * shutdown线程: CAS ready -> waiting_stop
         * shutdown线程: CAS running -> waiting_stop
         *
         * 由于状态总是向后流转, 故这 5 个原子操作无论在执行时序上如何交错, 最终总能被流转到终止状态.
         * 三方(execute线程、worker线程、shutdown线程)的语义不变性在并发情况可得到保证.
         * */
        int toState = force ? STOP : WAITING_STOP;
        if (state < toState) {
            if (compareAndSetState(INITIAL, toState)) {
                onCompleted();
            } else if (compareAndSetState(READY, toState)) {
                //这种情况,worker 正在启动过程中, 但是尚未开始执行任务.
                //如果状态改为 WAITING_STOP ,会致使 worker 执行完队列任务,继而执行 onCompleted.
                //如果状态改为 STOP, 会致使 worker 不执行任务, 直接执行 onCompleted
                //故这个if分支里什么也不用做
            } else {
                compareAndSetState(RUNNING, toState);
                workerThread.interrupt();
            }
        }
    }

    /**
     * 返回因 {@link this#shutdownNow()} 而未完成的任务
     */
    public List<Runnable> pendingTasks() {
        return this.queue.stream().toList();
    }

    public boolean terminated() {
        return state == TERMINATED;
    }

    private void startWorker() {
        if (state == INITIAL && compareAndSetState(INITIAL, READY)) {
            doStartWorker();
        }
    }

    private void doStartWorker() {
        executor.execute(() -> {
            workerThread = Thread.currentThread();
            compareAndSetState(READY, RUNNING);

            log.debug("Worker started");

            while (shouldWork()) {
                try {
                    Runnable task = queue.take();
                    task.run();
                } catch (InterruptedException e) {
                    /*
                     * 有两种可能导致 worker 线程被中断,
                     * 1. 用户代码直接调用了被当前实例包装的 executor 的 shutdown/shutdownNow 方法
                     * 2. 用户代码调用了当前实例的 shutdown/shutdownNow 方法
                     *
                     * 对于第一种, 这里忽略掉, 因此其不会影响 worker 的运行. 虽然“用户的感受是线程池关不掉”, 但这是其没有遵守协定导致.
                     * 第二种情况是预期的关闭线程池的正确操作.
                     */
                } catch (RuntimeException e) {
                    log.error("", e);
                }
            }
            onCompleted();
            log.debug("Worker stopped");
        });
    }

    private void onCompleted() {
        if (shouldWork()) {
            throw new IllegalStateException("state error");
        }
        /*
         * 当 shouldWork 返回 false, 意味着 state 状态已经不可变了,所以可以直接更新.
         * 但是存在幂等问题,针对这一点目前调用侧可以保证只调用一次.
         */
        state = TERMINATED;
        shutdownFuture.run();
    }

    private boolean shouldWork() {
        return state == RUNNING || (state == WAITING_STOP && !queue.isEmpty());
    }

    private boolean compareAndSetState(int expect, int update) {
        return STATE_UPDATER.compareAndSet(this, expect, update);
    }

    @Override
    public boolean inEventLoop() {
        return Thread.currentThread() == workerThread;
    }
}
