package io.github.light0x00.letty.core.eventloop;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author light0x00
 * @since 2023/6/27
 */
@AllArgsConstructor
public class SingleThreadPerTaskExecutor implements Executor {

    private ThreadFactory threadFactory;

    @Override
    public void execute(@NotNull Runnable command) {
        threadFactory.newThread(command).start();
    }
}
