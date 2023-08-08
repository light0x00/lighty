package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.eventloop.EventExecutorGroup;
import io.github.light0x00.lighty.core.handler.adapter.ChannelHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ChannelHandlerConfiguration {

    @Nullable
    EventExecutorGroup<?> executorGroup();

    @Nonnull
    List<? extends ChannelHandler> handlers();

    List<ChannelHandlerExecutorPair<ChannelHandler>> handlerExecutorPair();

}
