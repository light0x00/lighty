package io.github.light0x00.letty.expr.handler.adapter;

import io.github.light0x00.letty.expr.ChannelContext;
import io.github.light0x00.letty.expr.handler.ChannelObserver;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ChannelObserverAdapter implements ChannelObserver {

    @Override
    public void onConnected(ChannelContext context) {

    }

    @Override
    public void onReadCompleted(ChannelContext context) {

    }

    @Override
    public void onClosed(ChannelContext context) {

    }
}
