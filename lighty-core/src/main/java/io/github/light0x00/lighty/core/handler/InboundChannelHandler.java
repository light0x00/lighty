package io.github.light0x00.lighty.core.handler;

/**
 * 同一个channel的管道,始终只会被同一个线程执行,所以是"栈封闭"的,不具有共享性,自然线程安全.
 */
public interface InboundChannelHandler extends ChannelHandler {

    /**
     * Each time the readable event triggered, this method will be invoked, passing the accumulated bytes.
     * @param context the context of current channel.
     * @param data the data passed by the upstream of current phase.
     * @param next the downstream of current phase.
     */
    void onRead(ChannelContext context, Object data, InboundPipeline next);

}
