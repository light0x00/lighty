package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.handler.ChannelContext;
import io.github.light0x00.letty.core.handler.InboundPipeline;
import io.github.light0x00.letty.core.handler.adapter.ChannelHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/13
 */
@Slf4j
class ServerMessageHandler extends ChannelHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelContext context, Throwable th) {
        log.info("exceptionCaught", th);
        throw new RuntimeException("可爱的bug");
    }

    @Override
    public void onConnected(ChannelContext context) {
        context.channel().write("hello world")
                .addListener(
                        (f) -> context.channel().close()
                );

        throw new RuntimeException("无聊bug");

    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        log.info("onRead: {}", data);
    }
}
