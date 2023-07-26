package io.github.light0x00.letty.expr.concurrent;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author light0x00
 * @since 2023/6/16
 */
@Slf4j
public class ListenableFutureTask<T> extends FutureTask<T> {

    @GuardedBy("this")
    @Getter
    private final List<ListenerExecutorPair<T>> listeners = new LinkedList<>();

    /**
     * The default Executor to notify the listeners.
     * If null , the notifier may be either of the following:
     * - If a listener added before the FutureTask has been done, the runner who executes {@link #run} will be the notifier.
     * - Otherwise, the current thread who call {@link #addListener(FutureListener, Executor)} with null executor will be the notifier.
     * - Whenever a thread who call {@link #addListener(FutureListener, Executor)} with nonnull executor, then the specified executor will be notifier.
     */
    @Nullable
    private final Executor defaultNotifier;

    private volatile boolean hasDone;

    /**
     * For waite/notify scenarios
     */
    public ListenableFutureTask(@Nullable Executor defaultNotifier) {
        this(() -> {
        }, defaultNotifier);
    }

    public ListenableFutureTask(@Nonnull Callable<T> callable, @Nullable Executor defaultNotifier) {
        super(callable);
        this.defaultNotifier = defaultNotifier;
    }

    public ListenableFutureTask(@Nonnull Runnable runnable, @Nullable Executor defaultNotifier) {
        super(runnable, null);
        this.defaultNotifier = defaultNotifier;
    }

    @Override
    protected void done() {
        hasDone = true;
        notifyListeners();
    }

    @SneakyThrows   // we don't need the fucking checked exception, so here make it implicit. ^ ^
    public T get() {
        return super.get();
    }

    public void setSuccess() {
        setSuccess(null);
    }

    public void setSuccess(T t) {
        super.set(t);
    }

    public void setFailure(Throwable t) {
        super.setException(t);
    }

    public boolean isSuccess() {
        return cause() == null;
    }

    public Throwable cause() {
        try {
            super.get();
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (InterruptedException e) {
            return e;
        }
    }

    public ListenableFutureTask<T> addListener(FutureListener<T> listener) {
        return addListener(listener, defaultNotifier);
    }

    public ListenableFutureTask<T> addListener(FutureListener<T> listener, @Nullable Executor executor) {
        /*
        关于为什么这里不能用 FutureTask#isDone 作为判断依据的原因

        根据 FutureTask#set FutureTask#setException 的逻辑
        1. 更新 state 为 “done”
        2. 设置结果(可以是: outcome,exception,cancellation)
        3. 执行 done() 通知子类

        这个3个操作(整体)并非原子,并发环境这会产生如下执行时序:

         threadA: state = "done"
         threadB: if state = "done"
         threadB: notifyListener  # 此时 listener 调用 FutureTask#get() 将得到空的结果
         threadA: 设置future结果
         threadA: done()

         为了避免过早的 “done”, 需要将 “done” 的状态流转延迟到 “设置future结果” 之后
         这是为什么新增了额外的状态字段, 该状态在 done() 方法中变更为 “done”
         */
        if (hasDone) {
            notifyListener(new ListenerExecutorPair<>(listener, executor));
        } else {
            /*
             * 而当 done == false 时, 可能被并发读写, 此情况需加锁
             * The race condition is that `notifyListeners` may be call between the `If` and `Then` in `addListener`
             *
             * threadA: If not done
             * threadB: done()
             * threadB: notifyListeners()
             * threadA: Then list.add()
             *
             * To prevent this, what to do is make it mutually exclusive between `If Then act` and `notifyListeners`.
             * Here we use `this` as a mutex/
             * */
            synchronized (this) {
                if (hasDone) {
                    notifyListener(new ListenerExecutorPair<>(listener, executor));
                } else {
                    listeners.add(new ListenerExecutorPair<>(listener, executor));
                }
            }
        }
        return this;
    }

    private synchronized void notifyListeners() {
        for (ListenerExecutorPair<T> pair : listeners) {
            notifyListener(pair);
        }
    }

    private void notifyListener(ListenerExecutorPair<T> pair) {
        if (pair.executor == null) {
            pair.listener.operationComplete(this);
        } else {
            pair.executor.execute(() -> pair.listener.operationComplete(this));
        }
    }

    private record ListenerExecutorPair<T>(FutureListener<T> listener, @Nullable Executor executor) {

    }

    public static <T> ListenableFutureTask<List<ListenableFutureTask<T>>> all(List<ListenableFutureTask<T>> tasks) {
        ListenableFutureTask<List<ListenableFutureTask<T>>> listenableFutureTask = new ListenableFutureTask<>(
                () -> tasks, null);

        CountDownLatch countDownLatch = new CountDownLatch(tasks.size());
        for (ListenableFutureTask<?> task : tasks) {
            task.addListener((f) -> {
                countDownLatch.countDown();
                if (countDownLatch.getCount() == 0) {
                    listenableFutureTask.run();
                }
            });
        }

        return listenableFutureTask;
    }
}
