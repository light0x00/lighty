package io.github.light0x00.letty.expr.toolkit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author light0x00
 * @since 2023/6/29
 */
public interface Chooser<T> {
    T next();

    public static <T> Chooser<T> newChooser(T[] executors) {
        if (isPowerOfTwo(executors.length)) {
            return new PowerOfTwoChooser<>(executors);
        } else {
            return new GenericChooser<>(executors);
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    /**
     * @author light0x00
     * @since 2023/6/29
     */
    final class GenericChooser<T> implements Chooser<T> {
        private final AtomicLong idx = new AtomicLong();
        private final T[] executors;

        public GenericChooser(T[] executors) {
            this.executors = executors;
        }

        @Override
        public T next() {
            return executors[(int) Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }

    /**
     * @author light0x00
     * @since 2023/6/29
     */
    final class PowerOfTwoChooser<T> implements Chooser<T> {
        private final AtomicInteger idx = new AtomicInteger();
        private final T[] executors;

        public PowerOfTwoChooser(T[] executors) {
            this.executors = executors;
        }

        @Override
        public T next() {
            return executors[idx.getAndIncrement() & executors.length - 1];
        }
    }
}
