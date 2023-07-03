package io.github.light0x00.letty.expr;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 同一个channel的管道,始终只会被同一个线程执行,所以是"栈封闭"的,不具有共享性,自然线程安全.
 */
public interface ChannelOutboundPipeline {

    static OutboundInvocation buildInvocationChain(ChannelContext context, List<ChannelOutboundPipeline> pipelines, BiConsumer<Object, ListenableFutureTask<Void>> receiver) {
        OutboundInvocation invocation = receiver::accept;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            ChannelOutboundPipeline pipeline = pipelines.get(i);

            OutboundInvocation next = invocation;
            invocation = (data, f) -> pipeline.launch(context, data, next);
        }
        return invocation;

    }

    void launch(ChannelContext context, Object data, OutboundInvocation next);

}
