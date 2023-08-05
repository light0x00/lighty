package io.github.light0x00.letty.core.handler.adapter;

import io.github.light0x00.letty.core.handler.ChannelContext;

/**
 * @author light0x00
 * @since 2023/7/1
 */
public interface ChannelObserver {

    void exceptionCaught(ChannelContext context, Throwable th);

    void onConnected(ChannelContext context);

    void onReadCompleted(ChannelContext context);

    void onClosed(ChannelContext context);

}
