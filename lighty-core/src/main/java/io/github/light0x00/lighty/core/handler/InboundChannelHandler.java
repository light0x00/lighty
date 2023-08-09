package io.github.light0x00.lighty.core.handler;

/**
 * 同一个channel的管道,始终只会被同一个线程执行,所以是"栈封闭"的,不具有共享性,自然线程安全.
 */
public interface InboundChannelHandler extends ChannelHandler {

    void onRead(ChannelContext context, Object data, InboundPipeline next);

}
