package io.github.light0x00.letty.expr.eventloop;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * @author light0x00
 * @since 2023/6/27
 */
public class SingleThreadPerTaskExecutor implements Executor {

    @Override
    public void execute(@NotNull Runnable command) {
        new Thread(command).start();
    }
}
