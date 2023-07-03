package io.github.light0x00.letty.expr.eventloop;

/**
 * @author light0x00
 * @since 2023/6/29
 */
public interface EventExecutorGroup<T extends EventExecutor> {

    T next();
}
