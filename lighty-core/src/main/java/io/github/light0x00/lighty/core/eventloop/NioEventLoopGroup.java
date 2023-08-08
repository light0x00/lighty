package io.github.light0x00.lighty.core.eventloop;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author light0x00
 * @since 2023/6/16
 */
public class NioEventLoopGroup extends AbstractEventExecutorGroup<NioEventLoop> {

    public NioEventLoopGroup(int threadNum) {
        super(threadNum);
    }

    public NioEventLoopGroup(int threadNum, ThreadFactory threadFactory) {
        super(threadNum, threadFactory);
    }

    public NioEventLoopGroup(int threadNum, Executor executor) {
        super(threadNum, executor);
    }

    @Override
    protected NioEventLoop newEventLoop(Executor executor) {
        return new NioEventLoop(executor, this);
    }

}
