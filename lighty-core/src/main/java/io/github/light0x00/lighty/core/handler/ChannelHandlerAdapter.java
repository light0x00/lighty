package io.github.light0x00.lighty.core.handler;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ChannelHandlerAdapter implements ChannelHandler {

    @Override
    @Skip
    public void onInitialize(@Nonnull ChannelContext context) {

    }

    @Override
    @Skip
    public void onDestroy(@Nonnull ChannelContext context) {

    }

    @Override
    @Skip
    public void onConnected(@Nonnull ChannelContext context) {

    }

    @Override
    @Skip
    public void onReadCompleted(@Nonnull ChannelContext context) {

    }

    @Override
    @Skip
    public void onClosed(@Nonnull ChannelContext context) {

    }

    @Override
    @Skip
    public void exceptionCaught(@Nonnull ChannelContext context, @Nonnull Throwable t) {

    }
}
