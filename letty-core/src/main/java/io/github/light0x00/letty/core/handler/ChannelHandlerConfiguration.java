package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.eventloop.EventExecutorGroup;
import io.github.light0x00.letty.core.handler.adapter.ChannelHandler;

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
