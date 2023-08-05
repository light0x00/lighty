package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.eventloop.EventLoopGroup;
import io.github.light0x00.letty.core.handler.adapter.ChannelObserver;
import io.github.light0x00.letty.core.handler.adapter.InboundChannelHandler;
import io.github.light0x00.letty.core.handler.adapter.OutboundChannelHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public interface ChannelHandlerConfiguration {

    @Nullable
    EventLoopGroup<?> executorGroup();

    @Nonnull
    Collection<? extends ChannelObserver> observers();

    @Nonnull
    List<? extends InboundChannelHandler> inboundHandlers();

    @Nonnull
    List<? extends OutboundChannelHandler> outboundHandlers();

}
