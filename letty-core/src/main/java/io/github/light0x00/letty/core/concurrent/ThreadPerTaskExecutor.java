package io.github.light0x00.letty.core.concurrent;

import lombok.AllArgsConstructor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

@AllArgsConstructor
public final class ThreadPerTaskExecutor implements Executor {
    private final ThreadFactory threadFactory;

    @Override
    public void execute(Runnable command) {
        threadFactory.newThread(command).start();
    }
}
