package io.github.light0x00.lighty.codec;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.OutboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/8/27
 */
public abstract class OutboundMessageHandler<O> extends OutboundChannelHandlerAdapter {

    @Override
    public void onWrite(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull OutboundPipeline pipeline) {
        @SuppressWarnings("unchecked")
        var msg = (O) data;
        handleOutput(context, msg, pipeline);
    }

    protected abstract void handleOutput(@Nonnull ChannelContext context, @Nonnull O data, @Nonnull OutboundPipeline pipeline);

}
