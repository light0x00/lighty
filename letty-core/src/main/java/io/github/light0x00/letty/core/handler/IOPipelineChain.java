package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventLoop;
import io.github.light0x00.letty.core.util.Skip;
import io.github.light0x00.letty.core.util.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author light0x00
 * @since 2023/7/10
 */
@Slf4j
public class IOPipelineChain {

    InboundPipelineInvocation inboundChain;

    OutboundPipelineInvocation outboundChain;

    EventLoop eventLoop;

    public IOPipelineChain(
            EventLoop eventLoop,
            ChannelContext context,
            List<InboundChannelHandler> inboundHandlers,
            List<OutboundChannelHandler> outboundHandlers,
            BiConsumer<Object, ListenableFutureTask<Void>> receiver) {

        this.eventLoop = eventLoop;

        inboundHandlers = filterInboundHandlers(inboundHandlers);
        outboundHandlers = filterOutboundHandlers(outboundHandlers);

        if (inboundHandlers.isEmpty()) {
            log.warn("There is no inbound handler!");
        }
        if (outboundHandlers.isEmpty()) {
            log.warn("There is no outbound handler!");
        }

        inboundChain = InboundPipelineInvocation.buildInvocationChain(
                context, inboundHandlers);
        outboundChain = OutboundPipelineInvocation.buildInvocationChain(
                context, outboundHandlers, receiver);
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

    public void input(RecyclableByteBuffer buf) {
        if (eventLoop.inEventLoop()) {
            inboundChain.invoke(buf);
        } else {
            eventLoop.execute(() -> inboundChain.invoke(buf));
        }
    }

    public ListenableFutureTask<Void> output(Object data) {
        var writeFuture = new ListenableFutureTask<Void>(null);
        if (eventLoop.inEventLoop()) {
            outboundChain.invoke(data, writeFuture);
        } else {
            eventLoop.execute(() -> outboundChain.invoke(data, writeFuture));
        }
        return writeFuture;
    }
}
