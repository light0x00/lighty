package io.github.light0x00.letty.expr.handler;

/**
 * 同一个channel的管道,始终只会被同一个线程执行,所以是"栈封闭"的,不具有共享性,自然线程安全.
 */
public interface OutboundChannelHandler extends ChannelObserver {

    void onWrite(ChannelContext context, Object data, OutboundPipeline next);

}
