package io.github.light0x00.letty.core.handler;

import static io.github.light0x00.letty.core.util.Tool.stackTraceToString;

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
