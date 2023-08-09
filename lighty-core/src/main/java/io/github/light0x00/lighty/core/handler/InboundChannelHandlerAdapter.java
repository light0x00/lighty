package io.github.light0x00.lighty.core.handler;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class InboundChannelHandlerAdapter extends ChannelHandlerAdapter implements InboundChannelHandler {

    @Override
    @Skip
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {

    }
}
