package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;

import java.util.List;
import java.util.function.BiConsumer;

public interface OutboundPipelineInvocation {

    static OutboundPipelineInvocation buildInvocationChain(ChannelContext context, List<OutboundChannelHandler> pipelines, BiConsumer<Object, ListenableFutureTask<Void>> receiver) {
        OutboundPipelineInvocation invocation = receiver::accept;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            OutboundChannelHandler pipeline = pipelines.get(i);

            OutboundPipelineInvocation next = invocation;
            invocation = (dataIn, future) -> pipeline.onWrite(
                    context.nextContext(next),
                    dataIn,
                    (dataOut) -> next.invoke(dataOut, future));
        }
        return invocation;
    }

    void invoke(Object arg, ListenableFutureTask<Void> future);

}
