package io.github.light0x00.lighty.codec;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.DuplexChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/8/27
 */
public abstract class DuplexMessageHandler<I, O> extends DuplexChannelHandlerAdapter {
    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        @SuppressWarnings("unchecked")
        var msg = (I) data;
        handleInput(context, msg, pipeline);
    }

    @Override
    public void onWrite(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull OutboundPipeline pipeline) {
        @SuppressWarnings("unchecked")
        var msg = (O) data;
        handleOutput(context, msg, pipeline);
    }

    protected abstract void handleOutput(@Nonnull ChannelContext context, @Nonnull O message, @Nonnull OutboundPipeline pipeline);

    protected abstract void handleInput(@Nonnull ChannelContext context, @Nonnull I message, @Nonnull InboundPipeline pipeline);
}
