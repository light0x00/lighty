package io.github.light0x00.letty.core.concurrent;

import lombok.Getter;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

public class ListenableFutureTask<T> extends FutureTask<T> {

    @GuardedBy("this")
    @Getter
    private final List<ListenerExecutorPair<T>> listeners = new LinkedList<>();

    /**
     * For waite/notify scenarios
     */
    public ListenableFutureTask() {
        super(() -> {
        }, null);
    }

    public ListenableFutureTask(@Nonnull Callable<T> callable) {
        super(callable);
    }

    public ListenableFutureTask(@Nonnull Runnable runnable, T result) {
        super(runnable, result);
    }

    public ListenableFutureTask(@Nonnull Runnable runnable) {
        super(runnable, null);
    }

    @Override
    protected void done() {
        notifyListeners();
    }

    @SneakyThrows   // we don't need the fucking checked exception, so here make it implicit. ^ ^
    public T get() {
        return super.get();
    }

    @SneakyThrows
    public T await() {
        return super.get();
    }

    public ListenableFutureTask<T> addListener(FutureListener<T> listener, Executor executor) {
        /*
         * 当 isDone == true 时, 意味着 state 已经不可变, 故不存在 rc.
         * */
        if (isDone()) {
            listener.operationComplete(this);
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
                    listener.operationComplete(this);
                } else {

                    listeners.add(new ListenerExecutorPair<>(listener, executor));
                }
            }
        }
        return this;
    }

    private synchronized void notifyListeners() {
        for (ListenerExecutorPair<T> pair : listeners) {
            pair.executor.execute(() -> pair.listener.operationComplete(this));
        }
    }

    private record ListenerExecutorPair<T>(FutureListener<T> listener, Executor executor) {

    }
}
