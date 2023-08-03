package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.buffer.RecyclableBuffer;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventExecutor;
import io.github.light0x00.letty.core.util.Skip;
import io.github.light0x00.letty.core.util.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author light0x00
 * @since 2023/7/10
 */
@Slf4j
public class IOPipelineChain {

    InboundPipelineInvocation inboundChain;

    OutboundPipelineInvocation outboundChain;

    EventExecutor eventExecutor;

    public IOPipelineChain(
            EventExecutor eventExecutor,
            ChannelContext context,
            List<InboundChannelHandler> inboundHandlers,
            List<OutboundChannelHandler> outboundHandlers,
            OutboundPipelineInvocation receiver) {

        this.eventExecutor = eventExecutor;

        inboundHandlers = filterInboundHandlers(inboundHandlers);
        outboundHandlers = filterOutboundHandlers(outboundHandlers);

        inboundChain = InboundPipelineInvocation.buildInvocationChain(
                context, inboundHandlers, arg -> {
                    //the last phase
                });
        outboundChain = OutboundPipelineInvocation.buildInvocationChain(
                context, outboundHandlers, receiver);
    }

    public void input(RecyclableBuffer buf) {
        if (eventExecutor.inEventLoop()) {
            inboundChain.invoke(buf);
        } else {
            eventExecutor.execute(() -> inboundChain.invoke(buf));
        }
    }

    public ListenableFutureTask<Void> output(Object data) {
        var writeFuture = new ListenableFutureTask<Void>(null);
        if (eventExecutor.inEventLoop()) {
            outboundChain.invoke(data, writeFuture);
        } else {
            eventExecutor.execute(() -> outboundChain.invoke(data, writeFuture));
        }
        return writeFuture;
    }

    private static List<InboundChannelHandler> filterInboundHandlers(List<InboundChannelHandler> inboundHandlers) {
        return inboundHandlers.stream()
                .filter(ha ->
                        !Tool.methodExistAnnotation(Skip.class, ha.getClass(), "onRead", ChannelContext.class, Object.class, InboundPipeline.class)
                )
                .collect(Collectors.toList());
    }

    private static List<OutboundChannelHandler> filterOutboundHandlers(List<OutboundChannelHandler> inboundHandlers) {
        return inboundHandlers.stream()
                .filter(ha ->
                        !Tool.methodExistAnnotation(Skip.class, ha.getClass(), "onWrite", ChannelContext.class, Object.class, OutboundPipeline.class)
                )
                .collect(Collectors.toList());
    }
}
