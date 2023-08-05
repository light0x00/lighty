package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.handler.adapter.OutboundChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.light0x00.letty.core.util.Tool.stackTraceToString;

public interface OutboundPipelineInvocation {

    static OutboundPipelineInvocation buildInvocationChain(ChannelContext context,
                                                           List<? extends OutboundChannelHandler> pipelines,
                                                           OutboundPipelineInvocation receiver
    ) {
        OutboundPipelineInvocation invocation = receiver;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            OutboundChannelHandler pipeline = pipelines.get(i);

            OutboundPipelineInvocation next = invocation;
            invocation = new OutboundPipelineInvocationImpl(pipeline, context, next);
        }
        return invocation;
    }

    void invoke(Object arg, ListenableFutureTask<Void> future);

    @Slf4j
    class OutboundPipelineInvocationImpl implements OutboundPipelineInvocation {
        private final OutboundChannelHandler pipeline;
        private final ChannelContext context;
        private final OutboundPipelineInvocation next;

        public OutboundPipelineInvocationImpl(OutboundChannelHandler pipeline, ChannelContext context, OutboundPipelineInvocation next) {
            this.pipeline = pipeline;
            this.context = context;
            this.next = next;
        }

        @Override
        public void invoke(Object dataIn, ListenableFutureTask<Void> future) {
            try {
                pipeline.onWrite(
                        context.nextContext(next),
                        dataIn,
                        (dataOut) -> {
                            next.invoke(dataOut, future);
                            return future;
                        }
                );
            } catch (Throwable th) {
                invokeExceptionCaught(th);
            }
        }

        void invokeExceptionCaught(Throwable cause) {
            try {
                pipeline.exceptionCaught(context, cause);
            } catch (Throwable error) {
                log.warn("""
                                An exception {} was thrown by a user handler's exceptionCaught() method while handling the following exception:"""
                        , stackTraceToString(error), cause
                );
            }
        }
    }
}
