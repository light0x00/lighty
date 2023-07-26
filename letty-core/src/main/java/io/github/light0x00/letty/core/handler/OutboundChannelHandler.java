package io.github.light0x00.letty.core.handler;

public interface OutboundChannelHandler extends ChannelObserver {

    void onWrite(ChannelContext context, Object data, OutboundPipeline next);

}
