package io.github.light0x00.letty.expr;

import java.util.List;
import java.util.function.Consumer;

/**
 * 同一个channel的管道,始终只会被同一个线程执行,所以是"栈封闭"的,不具有共享性,自然线程安全.
 */
public interface ChannelInboundPipeline extends ChannelPipeline {

    static Invocation buildInvocationChain(ChannelContext context, List<ChannelInboundPipeline> pipelines, Consumer<Object> receiver) {
        Invocation invocation = receiver::accept;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            ChannelInboundPipeline pipeline = pipelines.get(i);

            Invocation next = invocation;
            invocation = data -> pipeline.launch(context, data, next);
        }
        return invocation;
    }

    void launch(ChannelContext context, Object data, Invocation next);

}
