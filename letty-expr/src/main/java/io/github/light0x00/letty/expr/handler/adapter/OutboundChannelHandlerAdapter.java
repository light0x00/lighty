package io.github.light0x00.letty.expr.handler.adapter;

import io.github.light0x00.letty.expr.ChannelContext;
import io.github.light0x00.letty.expr.Skip;
import io.github.light0x00.letty.expr.handler.OutboundChannelHandler;
import io.github.light0x00.letty.expr.handler.OutboundPipeline;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class OutboundChannelHandlerAdapter extends ChannelObserverAdapter implements OutboundChannelHandler {
    @Override
    @Skip
    public void onWrite(ChannelContext context, Object data, OutboundPipeline next) {

    }
}
