package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.util.EventLoopConfinement;

/**
 * @author light0x00
 * @since 2023/7/1
 */
@EventLoopConfinement
public interface ChannelHandler {

    void exceptionCaught(ChannelContext context, Throwable t);

    void onInitialize(ChannelContext context);

    void onDestroy(ChannelContext context);

    /**
     * Triggered when the 3-way handshake successful.
     */
    void onConnected(ChannelContext context);

    /**
     * Triggered when reach the end of input stream of current channel
     */
    void onReadCompleted(ChannelContext context);

    /**
     * Triggered when the 4-way handshake finished.
     */
    void onClosed(ChannelContext context);

}
