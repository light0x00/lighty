package io.github.light0x00.letty.expr.eventloop;

import io.github.light0x00.letty.expr.util.Chooser;
import lombok.SneakyThrows;

import java.util.concurrent.Executor;

import static io.github.light0x00.letty.expr.util.Chooser.newChooser;

/**
 * @author light0x00
 * @since 2023/6/16
 */
public class NioEventLoopGroup implements EventExecutorGroup<NioEventLoop> {

    private final NioEventLoop[] eventLoops;

    private final Chooser<NioEventLoop> eventExecutorChooser;

    public NioEventLoopGroup(int threadNum) {
        this(threadNum, new SingleThreadPerTaskExecutor());
    }

    @SneakyThrows
    public NioEventLoopGroup(int threadNum, Executor executor) {
        eventLoops = new NioEventLoop[threadNum];
        for (int i = 0; i < threadNum; i++) {
            eventLoops[i] = new NioEventLoop(executor, this);
        }
        eventExecutorChooser = newChooser(eventLoops);
    }

    public NioEventLoop next() {
        return eventExecutorChooser.next();
    }

}
