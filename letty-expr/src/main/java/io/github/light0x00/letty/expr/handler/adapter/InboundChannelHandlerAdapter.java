package io.github.light0x00.letty.expr.handler.adapter;

import io.github.light0x00.letty.expr.handler.ChannelContext;
import io.github.light0x00.letty.expr.util.Skip;
import io.github.light0x00.letty.expr.handler.InboundChannelHandler;
import io.github.light0x00.letty.expr.handler.InboundPipeline;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class InboundChannelHandlerAdapter extends ChannelObserverAdapter implements InboundChannelHandler {

    @Override
    @Skip
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {

    }
}
