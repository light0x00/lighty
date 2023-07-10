package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.util.Skip;
import io.github.light0x00.letty.expr.util.Tool;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;

public interface OutboundPipelineInvocation {

    static OutboundPipelineInvocation buildInvocationChain(ChannelContext context, List<OutboundChannelHandler> pipelines, BiConsumer<Object, ListenableFutureTask<Void>> receiver) {
        OutboundPipelineInvocation invocation = receiver::accept;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            OutboundChannelHandler pipeline = pipelines.get(i);

            if (Tool.existAnnotation(Skip.class, pipeline.getClass(), "onWrite", ChannelContext.class, Object.class, OutboundPipeline.class))
                continue;

            OutboundPipelineInvocation next = invocation;
            invocation = (dataIn, future) -> pipeline.onWrite(
                    new DelegateChannelContext(context) {
                        @NotNull
                        @Override
                        public ListenableFutureTask<Void> write(@NotNull Object data) {
                            next.invoke(data, future); //沿用这个 future , 好处是会让上游也得到通知; 而如果重新 new, 那么上游的 addListener 将没有意义
                            return future;
                        }
                    },
                    dataIn,
                    (dataOut) -> next.invoke(dataOut, future));
        }
        return invocation;
    }

    void invoke(Object arg, ListenableFutureTask<Void> future);

}
