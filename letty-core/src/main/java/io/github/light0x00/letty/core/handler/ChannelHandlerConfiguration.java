package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.eventloop.EventLoopGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ChannelHandlerConfiguration {

    @Nullable
    default EventLoopGroup<?> handlerExecutor() {
        return null;
    }

    @Nonnull
    List<InboundChannelHandler> inboundHandlers();

    @Nonnull
    List<OutboundChannelHandler> outboundHandlers();

}
