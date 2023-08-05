package io.github.light0x00.letty.core.handler.adapter;

import io.github.light0x00.letty.core.handler.ChannelContext;
import io.github.light0x00.letty.core.handler.OutboundPipeline;

public interface OutboundChannelHandler extends ChannelObserver {

    void onWrite(ChannelContext context, Object data, OutboundPipeline next);

}
