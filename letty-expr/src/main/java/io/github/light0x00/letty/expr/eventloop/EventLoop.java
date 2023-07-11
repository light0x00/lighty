package io.github.light0x00.letty.expr.eventloop;

import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;

import java.util.concurrent.Executor;

/**
 * @author light0x00
 * @since 2023/6/30
 */
public interface EventLoop extends Executor {

    boolean inEventLoop();

    ListenableFutureTask<Void> shutdown();
}
