package io.github.light0x00.lighty.core.eventloop;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
public class DefaultEventLoopGroup extends AbstractEventExecutorGroup<SingleThreadExecutor> {

    public DefaultEventLoopGroup(int threadNum) {
        super(threadNum);
    }

    public DefaultEventLoopGroup(int threadNum, ThreadFactory threadFactory) {
        super(threadNum, threadFactory);
    }

    public DefaultEventLoopGroup(int threadNum, Executor executor) {
        super(threadNum, executor);
    }

    @Override
    protected SingleThreadExecutor newEventLoop(Executor executor) {
        return new SingleThreadExecutor(executor);
    }
}
