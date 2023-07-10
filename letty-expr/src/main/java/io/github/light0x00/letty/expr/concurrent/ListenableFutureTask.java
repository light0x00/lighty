package io.github.light0x00.letty.expr.concurrent;

import lombok.Getter;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author light0x00
 * @since 2023/6/16
 */
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

    AtomicBoolean hasRun = new AtomicBoolean();

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
        if (hasRun.compareAndSet(false, true))
            notifyListeners();
    }

    @SneakyThrows   // we don't need the fucking checked exception, so here make it implicit. ^ ^
    public T get() {
        return super.get();
    }

    public boolean isSuccess() {
        return cause() == null;
    }

    public Throwable cause() {
        try {
            super.get();
            return null;
        } catch (ExecutionException | InterruptedException e) {
            return e;
        }
    }

    public ListenableFutureTask<T> addListener(FutureListener<T> listener) {
        return addListener(listener, defaultNotifier);
    }

    public ListenableFutureTask<T> addListener(FutureListener<T> listener, @Nullable Executor executor) {
        /*
         * 当 isDone == true 时, 意味着 state 已经不可变, 故不存在 rc.
         * */
        if (isDone()) {
            notifyListener(new ListenerExecutorPair<>(listener, executor));
        } else {
            /*
             * 而当 isDone == false 时, 可能被并发读写, 此情况需加锁
             * The race condition is that `notifyListeners` may be call between the `If` and `Then` in `addListener`
             *
             * threadA: If not done
             * threadB: done
             * threadB: notifyListeners
             * threadA: Then list.add()
             *
             * To prevent this, what to do is make it mutually exclusive between `If Then act` and `notifyListeners`.
             * Here we use `this` as a mutex/
             * */
            synchronized (this) {
                if (isDone()) {
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
}
