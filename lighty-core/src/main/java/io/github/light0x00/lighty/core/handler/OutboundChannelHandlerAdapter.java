package io.github.light0x00.lighty.core.handler;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class OutboundChannelHandlerAdapter extends ChannelHandlerAdapter implements OutboundChannelHandler {
    @Override
    @Skip
    public void onWrite(ChannelContext context, Object data, OutboundPipeline next) {

    }
}
