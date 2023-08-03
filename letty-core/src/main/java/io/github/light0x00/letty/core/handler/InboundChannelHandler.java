package io.github.light0x00.letty.core.handler;

import lombok.SneakyThrows;

import java.lang.reflect.Method;

/**
 * 同一个channel的管道,始终只会被同一个线程执行,所以是"栈封闭"的,不具有共享性,自然线程安全.
 */
public interface InboundChannelHandler extends ChannelObserver {

    void onRead(ChannelContext context, Object data, InboundPipeline next);


    @SneakyThrows
    static Method getOnReadMethod(InboundChannelHandler handler) {
        return handler.getClass().getMethod("onRead", ChannelContext.class, Object.class, InboundPipeline.class);
    }
}
