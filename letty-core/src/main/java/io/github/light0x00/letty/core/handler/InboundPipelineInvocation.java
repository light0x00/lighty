package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.handler.adapter.InboundChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.light0x00.letty.core.util.Tool.stackTraceToString;

public interface InboundPipelineInvocation {

    static InboundPipelineInvocation buildInvocationChain(ChannelContext context,
                                                          List<? extends InboundChannelHandler> pipelines,
                                                          InboundPipelineInvocation receiver
    ) {
        InboundPipelineInvocation invocation = receiver;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            InboundChannelHandler pipeline = pipelines.get(i);

            InboundPipelineInvocation next = invocation;
            invocation = new InboundPipelineInvocationImpl(pipeline, context, next);

        }
        return invocation;
    }

    void invoke(Object arg);

    @Slf4j
    class InboundPipelineInvocationImpl implements InboundPipelineInvocation {
        private final InboundChannelHandler pipeline;
        private final ChannelContext context;
        private final InboundPipelineInvocation next;

        public InboundPipelineInvocationImpl(InboundChannelHandler pipeline, ChannelContext context, InboundPipelineInvocation next) {
            this.pipeline = pipeline;
            this.context = context;
            this.next = next;
        }

        @Override
        public void invoke(Object data) {
            try {
                pipeline.onRead(context, data, next::invoke);
            } catch (Throwable th) {
                invokeExceptionCaught(th);
            }
        }

        void invokeExceptionCaught(Throwable cause) {
            try {
                pipeline.exceptionCaught(context, cause);
            } catch (Throwable error) {
                log.warn("""
                                An exception {} was thrown by a user handler's exceptionCaught() method:
                                while handling the following exception:"""
                        , stackTraceToString(error), cause
                );
            }
        }

    }
}
