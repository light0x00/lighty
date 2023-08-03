package io.github.light0x00.letty.core.handler;

import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public interface OutboundChannelHandler extends ChannelObserver {

    void onWrite(ChannelContext context, Object data, OutboundPipeline next);

    @SneakyThrows
    @Nonnull
    static Method getMethodOnWrite(@Nonnull OutboundChannelHandler handler) {
        return handler.getClass().getMethod(
                "onWrite", ChannelContext.class, Object.class, OutboundPipeline.class);
    }
}
