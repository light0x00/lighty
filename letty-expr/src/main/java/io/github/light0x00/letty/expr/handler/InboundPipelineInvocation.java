package io.github.light0x00.letty.expr.handler;

import java.util.List;

public interface InboundPipelineInvocation {

    static InboundPipelineInvocation buildInvocationChain(ChannelContext context, List<InboundChannelHandler> pipelines) {
        InboundPipelineInvocation invocation = arg -> {
        };
        if(pipelines.isEmpty()){
            //todo 如果为0 则 warn 日志

        }

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            InboundChannelHandler pipeline = pipelines.get(i);

            InboundPipelineInvocation next = invocation;
            invocation = data -> pipeline.onRead(context, data, next::invoke);
        }
        return invocation;
    }

    void invoke(Object arg);

}
