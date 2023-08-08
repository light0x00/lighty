package io.github.light0x00.lighty.core.handler.adapter;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;

public interface OutboundChannelHandler extends ChannelHandler {

    void onWrite(ChannelContext context, Object data, OutboundPipeline next);

}
