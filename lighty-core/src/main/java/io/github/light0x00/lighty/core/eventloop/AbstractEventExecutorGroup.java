package io.github.light0x00.lighty.core.eventloop;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.util.Chooser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * @author light0x00
 * @since 2023/7/11
 */
@Slf4j
public abstract class AbstractEventExecutorGroup<T extends EventExecutor> implements EventExecutorGroup<T> {
    private final T[] eventLoops;

    private final Chooser<T> eventExecutorChooser;

    private final ListenableFutureTask<Void> shutdownFuture = new ListenableFutureTask<>(null);

    public AbstractEventExecutorGroup(int threadNum) {
        this(threadNum, (ThreadFactory) Thread::new);
    }

    public AbstractEventExecutorGroup(int threadNum, ThreadFactory threadFactory) {
        this(threadNum, new SingleThreadPerTaskExecutor(threadFactory));
    }

    @SneakyThrows
    public AbstractEventExecutorGroup(int threadNum, Executor executor) {
        //noinspection unchecked
        eventLoops = (T[]) new EventExecutor[2];

        for (int i = 0; i < threadNum; i++) {
            eventLoops[i] = newEventLoop(executor);
        }
        eventExecutorChooser = Chooser.newChooser(eventLoops);
    }

    protected abstract T newEventLoop(Executor executor);

    @Override
    public T next() {
        return eventExecutorChooser.next();
    }

    @Override
    public ListenableFutureTask<Void> shutdown() {
        List<ListenableFutureTask<Void>> shutdownFutures = Arrays.stream(eventLoops)
                .map(EventExecutor::shutdown)
                .collect(Collectors.toList());

        ListenableFutureTask.all(shutdownFutures).addListener(f -> {
            List<ListenableFutureTask<Void>> futures = f.get();
            for (ListenableFutureTask<Void> future : futures) {
                try {
                    future.get();
                } catch (Throwable t) {
                    log.error("Error occurred while shutdown event loop", t);
                }
            }
            shutdownFuture.run();
            log.debug("Event Loop group shutdown completed");

        });
        return shutdownFuture;
    }
}
