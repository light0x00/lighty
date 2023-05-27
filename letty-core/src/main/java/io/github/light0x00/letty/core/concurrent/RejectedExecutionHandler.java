package io.github.light0x00.letty.core.concurrent;

/**
 * Similar to {@link java.util.concurrent.RejectedExecutionHandler} but specific to {@link EventExecutor}.
 */
public interface RejectedExecutionHandler {

    /**
     * Called when someone tried to add a task to {@link EventExecutor} but this failed due capacity
     * restrictions.
     */
    void rejected(Runnable task, EventExecutor executor);
}
