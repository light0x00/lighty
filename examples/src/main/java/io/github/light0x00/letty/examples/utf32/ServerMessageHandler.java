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
    public void onError(ChannelContext context, Throwable th) {

    }

    @Override
    public void onConnected(ChannelContext context) {
        context.channel().write("hello world")
                .addListener(
                        (f) -> context.channel().close()
                );

        context.channel().close();
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        log.info("onRead: {}", data);
    }
}
