package io.github.light0x00.letty.old.expr;

import io.github.light0x00.letty.old.concurrent.SingleThreadPerTaskExecutor;
import lombok.SneakyThrows;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author light0x00
 * @since 2023/6/16
 */
public class EventLoopGroup {

    private final EventLoop[] eventLoops;

    private final EventExecutorChooser eventExecutorChooser;

    @SneakyThrows
    public EventLoopGroup(int threadNum, ThreadFactory threadFactory) {
        this(threadNum, new SingleThreadPerTaskExecutor(threadFactory));
    }

    @SneakyThrows
    public EventLoopGroup(int threadNum, Executor executor) {
        eventLoops = new EventLoop[threadNum];
        for (int i = 0; i < threadNum; i++) {
            eventLoops[i] = new EventLoop(executor);
        }
        eventExecutorChooser = newChooser(eventLoops);
    }

    public EventLoop next() {
        return eventExecutorChooser.next();
    }

    interface EventExecutorChooser {
        EventLoop next();
    }


    private static EventExecutorChooser newChooser(EventLoop[] executors) {
        if (isPowerOfTwo(executors.length)) {
            return new PowerOfTwoEventExecutorChooser(executors);
        } else {
            return new GenericEventExecutorChooser(executors);
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {
        private final AtomicInteger idx = new AtomicInteger();
        private final EventLoop[] executors;

        PowerOfTwoEventExecutorChooser(EventLoop[] executors) {
            this.executors = executors;
        }

        @Override
        public EventLoop next() {
            return executors[idx.getAndIncrement() & executors.length - 1];
        }
    }

    private static final class GenericEventExecutorChooser implements EventExecutorChooser {
        private final AtomicLong idx = new AtomicLong();
        private final EventLoop[] executors;

        GenericEventExecutorChooser(EventLoop[] executors) {
            this.executors = executors;
        }

        @Override
        public EventLoop next() {
            return executors[(int) Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }
}
