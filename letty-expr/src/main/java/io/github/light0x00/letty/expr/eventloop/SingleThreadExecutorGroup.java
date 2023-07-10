package io.github.light0x00.letty.expr.eventloop;

import io.github.light0x00.letty.expr.util.Chooser;

import java.util.concurrent.Executor;

/**
 * @author light0x00
 * @since 2023/6/29
 */
public class SingleThreadExecutorGroup implements EventExecutorGroup<SingleThreadExecutor> {

    private SingleThreadExecutor[] executors;

    private final Chooser<SingleThreadExecutor> eventExecutorChooser;

    public SingleThreadExecutorGroup(int threadNum) {
        this(threadNum, new SingleThreadPerTaskExecutor());
    }

    public SingleThreadExecutorGroup(int threadNum, Executor executor) {
        executors = new SingleThreadExecutor[threadNum];
        for (int i = 0; i < threadNum; i++) {
            executors[i] = new SingleThreadExecutor(executor);
        }
        eventExecutorChooser = Chooser.newChooser(executors);
    }

    @Override
    public SingleThreadExecutor next() {
        return eventExecutorChooser.next();
    }

}
