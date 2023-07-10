package io.github.light0x00.letty.expr.handler.adapter;

import io.github.light0x00.letty.expr.handler.ChannelObserver;
import io.github.light0x00.letty.expr.handler.InboundChannelHandler;
import io.github.light0x00.letty.expr.handler.OutboundChannelHandler;

/**
 * @author light0x00
 * @since 2023/7/10
 */
public interface ChannelHandler extends ChannelObserver, InboundChannelHandler, OutboundChannelHandler {
}
