package io.github.light0x00.letty.expr.light0x00.letty.core;

import io.github.light0x00.letty.core.concurrent.EventLoopExecutor;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

@Slf4j
public class ListenableFutureTaskTest {


    /**
     * 测试与兼容性
     */
    @Test
    public void test() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "listener-executor"));

        executor.submit(
                new ListenableFutureTask<>()
                        .addListener((f) -> {
                            Assertions.assertEquals("listener-executor", Thread.currentThread().getName());
                            log.info("{}", f.get());
                        }, listenerExecutor)
        );

        executor.shutdown();
        listenerExecutor.shutdown();
    }

    /**
     * 1. 测试执行 listener 的线程是否正确
     * 2. 测试是否执行 listener
     * 3. 测试与 {@link EventLoopExecutor} 的兼容性
     */
    @SneakyThrows
    @Test
    public void test2() {
        //执行 future 的线程
        EventLoopExecutor eventExecutor = new EventLoopExecutor(command -> new Thread(command).start());
        //执行 listener 的线程
        ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "listener-executor"));

        ListenableFutureTask<Double> future = eventExecutor.submit(Math::random);

        CountDownLatch executed = new CountDownLatch(1);

        future.addListener(futureTask -> {
            Assertions.assertEquals("listener-executor", Thread.currentThread().getName(), "listener 的执行线程错误!");     //测试执行 listener 的线程是否正确
            executed.countDown();
            log.info("execute result: {}", futureTask.get());
        }, listenerExecutor);

        Assertions.assertNotNull(future.await());
        executed.await();
        Assertions.assertTrue(executed.await(1, TimeUnit.SECONDS), "listener 没有被执行!");  //测试是否执行 listener

        //gracefully shutdown
        eventExecutor.shutdown().await();
    }
}
