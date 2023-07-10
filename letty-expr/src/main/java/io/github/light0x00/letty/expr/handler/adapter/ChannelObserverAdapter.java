package io.github.light0x00.letty.expr.handler.adapter;

import io.github.light0x00.letty.expr.handler.ChannelContext;
import io.github.light0x00.letty.expr.util.Skip;
import io.github.light0x00.letty.expr.handler.ChannelObserver;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ChannelObserverAdapter implements ChannelObserver {

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
}
