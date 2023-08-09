package io.github.light0x00.lighty.core.handler.adapter;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;
import io.github.light0x00.lighty.core.util.Skip;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class DuplexChannelHandlerAdapter extends ChannelHandlerAdapter implements DuplexChannelHandler {

    @Override
    @Skip
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
    }

    @Override
    @Skip
    public void onWrite(ChannelContext context, Object data, OutboundPipeline next) {
    }
}