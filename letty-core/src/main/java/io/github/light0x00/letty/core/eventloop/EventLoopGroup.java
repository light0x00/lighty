package io.github.light0x00.letty.core.eventloop;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;

/**
 * @author light0x00
 * @since 2023/6/29
 */
public interface EventLoopGroup<T extends EventExecutor> {

    T next();

    ListenableFutureTask<Void> shutdown();
}
