package io.github.light0x00.lighty.examples.utf32;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.core.handler.adapter.DuplexChannelHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/13
 */
@Slf4j
class ClientMessageHandler extends DuplexChannelHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelContext context, Throwable t) {

    }

    @Override
    public void onConnected(ChannelContext context) {
//        context.channel().write("hello world");
//        context.channel().close();
        log.info("{}", context.channel().localAddress());
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        log.info("onRead: {}", data);

        context.channel().write("ACK:" + data);
    }
}