package io.github.light0x00.letty.core.handler.adapter;

import io.github.light0x00.letty.core.handler.ChannelContext;
import io.github.light0x00.letty.core.util.Skip;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ChannelHandlerAdapter implements ChannelHandler {

    @Override
    @Skip
    public void onConnected(ChannelContext context) {
        
    }

    @Override
    @Skip
    public void onReadCompleted(ChannelContext context) {

    }

    @Override
    @Skip
    public void onClosed(ChannelContext context) {

    }

    @Override
    @Skip
    public void exceptionCaught(ChannelContext context, Throwable t) {

    }
}
