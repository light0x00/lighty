package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.util.EventLoopConfinement;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/7/1
 */
@EventLoopConfinement
public interface ChannelHandler {

    void onInitialize(@Nonnull ChannelContext context);

    void onDestroy(@Nonnull ChannelContext context);

    /**
     * Triggered when the 3-way handshake successful.
     */
    void onConnected(@Nonnull ChannelContext context);

    /**
     * Triggered when reach the end of input stream of current channel
     */
    void onReadCompleted(@Nonnull ChannelContext context);

    /**
     * Triggered when the 4-way handshake finished.
     */
    void onClosed(@Nonnull ChannelContext context);

    void exceptionCaught(@Nonnull ChannelContext context, @Nonnull Throwable t);

}
