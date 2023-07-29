package io.github.light0x00.letty.old.concurrent;

/**
 * Similar to {@link java.util.concurrent.RejectedExecutionHandler} but specific to {@link EventLoopExecutor}.
 */
public interface RejectedExecutionHandler {

    /**
     * Called when someone tried to add a task to {@link EventLoopExecutor} but this failed due capacity
     * restrictions.
     */
    void rejected(Runnable task, EventLoopExecutor executor);
}
