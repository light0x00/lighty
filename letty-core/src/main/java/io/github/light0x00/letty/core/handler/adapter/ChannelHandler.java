package io.github.light0x00.letty.core.handler.adapter;

import io.github.light0x00.letty.core.handler.ChannelObserver;
import io.github.light0x00.letty.core.handler.InboundChannelHandler;
import io.github.light0x00.letty.core.handler.OutboundChannelHandler;

/**
 * @author light0x00
 * @since 2023/7/10
 */
public interface ChannelHandler extends ChannelObserver, InboundChannelHandler, OutboundChannelHandler {
}
