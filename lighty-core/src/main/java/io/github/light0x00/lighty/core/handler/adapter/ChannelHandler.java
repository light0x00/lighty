package io.github.light0x00.lighty.core.handler.adapter;

import io.github.light0x00.lighty.core.handler.ChannelContext;

/**
 * @author light0x00
 * @since 2023/7/1
 */
public interface ChannelHandler {

    void exceptionCaught(ChannelContext context, Throwable t);

    void onConnected(ChannelContext context);

    void onReadCompleted(ChannelContext context);

    void onClosed(ChannelContext context);

}
