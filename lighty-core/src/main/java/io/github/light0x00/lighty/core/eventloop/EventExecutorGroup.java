package io.github.light0x00.lighty.core.eventloop;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

/**
 * @author light0x00
 * @since 2023/6/29
 */
public interface EventExecutorGroup<T extends EventExecutor> {

    T next();

    ListenableFutureTask<Void> shutdown();
}
