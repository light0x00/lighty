package io.github.light0x00.lighty.core.handler;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class OutboundChannelHandlerAdapter extends ChannelHandlerAdapter implements OutboundChannelHandler {
    @Override
    @Skip
    public void onWrite(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull OutboundPipeline pipeline) {

    }
}
