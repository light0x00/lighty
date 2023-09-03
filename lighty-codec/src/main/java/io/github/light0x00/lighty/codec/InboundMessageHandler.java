package io.github.light0x00.lighty.codec;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/8/27
 */
public abstract class InboundMessageHandler<I> extends InboundChannelHandlerAdapter {

    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        @SuppressWarnings("unchecked")
        var msg = (I) data;
        handleInput(context, msg, pipeline);
    }

    protected abstract void handleInput(@Nonnull ChannelContext context, @Nonnull I message, @Nonnull InboundPipeline pipeline);
}
