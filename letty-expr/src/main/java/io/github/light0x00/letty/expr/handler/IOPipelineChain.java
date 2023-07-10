package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author light0x00
 * @since 2023/7/10
 */
public class IOPipelineChain {

    InboundPipelineInvocation inboundChain;

    OutboundPipelineInvocation outboundChain;

    public IOPipelineChain(ChannelContext context, List<InboundChannelHandler> inboundPipelines,
                           List<OutboundChannelHandler> outboundPipelines, BiConsumer<Object, ListenableFutureTask<Void>> writter) {
        inboundChain = InboundPipelineInvocation.buildInvocationChain(
                context, inboundPipelines);
        outboundChain = OutboundPipelineInvocation.buildInvocationChain(
                context, outboundPipelines, writter);
    }

    public void input(RecyclableByteBuffer buf){
        inboundChain.invoke(buf);
    }

    public ListenableFutureTask<Void> output(Object data){
        var writeFuture = new ListenableFutureTask<Void>(null);
        outboundChain.invoke(data, writeFuture);
        return writeFuture;
    }
}
