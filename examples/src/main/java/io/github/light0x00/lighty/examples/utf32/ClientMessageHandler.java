package io.github.light0x00.lighty.examples.utf32;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.DuplexChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/13
 */
@Slf4j
class ClientMessageHandler extends DuplexChannelHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelContext context, Throwable t) {
        log.info("exceptionCaught",t);
    }

    @Override
    public void onConnected(ChannelContext context) {
        log.info("onConnected: {}", context.channel().remoteAddress());
    }

    @Override
    public void onClosed(ChannelContext context) {
        log.info("onClosed: {}",context.channel());
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        log.info("Received message: {}", data);
        context.channel().writeAndFlush("ACK:" + data);
    }
}
