package io.github.light0x00.letty.core;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

@Slf4j
public class ConcurrentTestUtil {

    public static ListenableFutureTask<Void> execConcurrently(ThreadFactory threadFactory, int repetition, Runnable... tasks) {
        return execConcurrently((threadId) -> tasks[threadId], tasks.length, repetition, threadFactory);
    }

    public static ListenableFutureTask<Void> execConcurrently(Runnable task, int threadNum, int repetition) {
        int[] id = new int[1];
        return execConcurrently(task, threadNum, repetition, (r) -> new Thread(r, "Concurrent-Test-Thread-" + id[0]++));
    }

    public static ListenableFutureTask<Void> execConcurrently(Runnable task, int threadNum, int repetition, ThreadFactory threadFactory) {
        return execConcurrently(threadId -> task, threadNum, repetition, threadFactory);
    }

    public static ListenableFutureTask<Void> execConcurrently(Function<Integer, Runnable> threadTaskSupplier, int threadNum, int repetition, ThreadFactory threadFactory) {
        var future = new ListenableFutureTask<Void>();
        CyclicBarrier beginBarrier = new CyclicBarrier(threadNum, () -> {

        });

        CyclicBarrier finishBarrier = new CyclicBarrier(threadNum, future);

        for (int i = 0; i < threadNum; i++) {
            int threadId = i;
            threadFactory.newThread(() -> {
                try {
                    beginBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
                Runnable task = threadTaskSupplier.apply(threadId);
                for (int j = 0; j < repetition; j++) {
                    task.run();
                }
                try {
                    finishBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }

            }).start();
        }
        return future;
    }
}
