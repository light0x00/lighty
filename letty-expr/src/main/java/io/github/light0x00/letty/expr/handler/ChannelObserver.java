package io.github.light0x00.letty.expr.handler;

/**
 * @author light0x00
 * @since 2023/7/1
 */
public interface ChannelObserver {
    void onConnected(ChannelContext context);

    void onReadCompleted(ChannelContext context);

    void onClosed(ChannelContext context);

}
