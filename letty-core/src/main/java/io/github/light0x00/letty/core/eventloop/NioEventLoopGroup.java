package io.github.light0x00.letty.core.eventloop;

import lombok.SneakyThrows;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author light0x00
 * @since 2023/6/16
 */
public class NioEventLoopGroup extends AbstractEventLoopGroup<NioEventLoop> {

    public NioEventLoopGroup(int threadNum) {
        super(threadNum);
    }

    public NioEventLoopGroup(int threadNum, ThreadFactory threadFactory) {
        super(threadNum, threadFactory);
    }

    @SneakyThrows
    public NioEventLoopGroup(int threadNum, Executor executor) {
        super(threadNum, executor);
    }

    @Override
    protected NioEventLoop newEventLoop(Executor executor) {
        return new NioEventLoop(executor, this);
    }

}
