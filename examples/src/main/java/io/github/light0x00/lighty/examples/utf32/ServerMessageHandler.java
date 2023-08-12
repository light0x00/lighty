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
class ServerMessageHandler extends DuplexChannelHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelContext context, Throwable t) {
        log.info("exceptionCaught", t);
    }

    @Override
    public void onConnected(ChannelContext context) {
        log.info("Connection established");
        context.channel().writeAndFlush("hello world")
                .addListener(
                        (f) -> log.info("write result:{}", f.isSuccess())
                );
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        log.info("Message received: {}", data);
        context.channel().close();
    }

}
