package io.github.light0x00.letty.expr.eventloop;

import java.util.concurrent.Executor;

/**
 * @author light0x00
 * @since 2023/6/30
 */
public interface EventExecutor extends Executor {

    boolean inEventLoop();
}
