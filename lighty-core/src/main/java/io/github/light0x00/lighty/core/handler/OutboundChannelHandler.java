package io.github.light0x00.lighty.core.handler;

public interface OutboundChannelHandler extends ChannelHandler {

    void onWrite(ChannelContext context, Object data, OutboundPipeline next);

}
