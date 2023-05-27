package io.github.light0x00.letty.core;

import io.github.light0x00.letty.core.concurrent.EventExecutor;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class EventExecutorTest {

    @Test
    public void testShutdownBeforeExecute() {
        AtomicInteger rejectCount = new AtomicInteger();
        AtomicInteger exeCount = new AtomicInteger();

        EventExecutor eventExecutor = new EventExecutor(
                command -> new Thread(command).start(),
                (task, executor) -> rejectCount.incrementAndGet()
        );

        ListenableFutureTask<Void> promise = eventExecutor.shutdown();
        eventExecutor.submit(exeCount::incrementAndGet);

        promise.get();

        Assertions.assertEquals(0, exeCount.get());
        Assertions.assertEquals(1, rejectCount.get());
        Assertions.assertTrue(eventExecutor.terminated());

        log.info("投入任务次数:{},执行任务次数:{},拒绝任务次数:{},队列剩余任务数量:{},是否终止状态:{}",
                1,
                exeCount.get(),
                rejectCount.get(),
                eventExecutor.pendingTasks().size(),
                eventExecutor.terminated()
        );
    }

    @Test
    public void testShutdownAfterExecute() {
        AtomicInteger rejectCount = new AtomicInteger();
        AtomicInteger exeCount = new AtomicInteger();

        EventExecutor eventExecutor = new EventExecutor(
                command -> new Thread(command).start(),
                (task, executor) -> rejectCount.incrementAndGet()
        );

        //1. 先投递任务到队列
        eventExecutor.submit(exeCount::incrementAndGet);
        //2. 后正常 shutdown
        ListenableFutureTask<Void> promise = eventExecutor.shutdown();

        promise.get();

        Assertions.assertEquals(1, exeCount.get());
        Assertions.assertEquals(0, rejectCount.get());
        Assertions.assertTrue(eventExecutor.terminated());

        log.info("投入任务次数:{},执行任务次数:{},拒绝任务次数:{},队列剩余任务数量:{},是否终止状态:{}",
                1,
                exeCount.get(),
                rejectCount.get(),
                eventExecutor.pendingTasks().size(),
                eventExecutor.terminated()
        );
    }

    @Test
    public void testExecuteAndShutdownConcurrently() throws InterruptedException {
        AtomicInteger putCount = new AtomicInteger();
        AtomicInteger rejectCount = new AtomicInteger();
        AtomicInteger exeCount = new AtomicInteger();


        EventExecutor eventExecutor = new EventExecutor(
                command -> new Thread(command).start(),
                (task, executor) -> rejectCount.incrementAndGet()
        );

        ConcurrentTestUtil.execConcurrently(() -> {
            putCount.incrementAndGet();
            eventExecutor.execute(() -> {
                exeCount.incrementAndGet();
            });
        }, 8, 100);

        Thread.sleep(2);

        ListenableFutureTask<Void> promise = eventExecutor.shutdown();
        promise.get();

        log.info("投入任务次数:{},执行任务次数:{},拒绝任务次数:{},队列剩余任务数量:{},是否终止状态:{}",
                putCount.get(),
                exeCount.get(),
                rejectCount.get(),
                eventExecutor.pendingTasks().size(),
                eventExecutor.terminated()
        );

        Assertions.assertEquals(putCount.get(), exeCount.get() + rejectCount.get());
    }

}
