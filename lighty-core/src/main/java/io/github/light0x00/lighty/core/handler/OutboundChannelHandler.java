package io.github.light0x00.lighty.core.handler;

import javax.annotation.Nonnull;

public interface OutboundChannelHandler extends ChannelHandler {

    void onWrite(@Nonnull ChannelContext context,@Nonnull Object data,@Nonnull OutboundPipeline pipeline);

}
