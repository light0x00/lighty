package io.github.light0x00.letty.expr.examples.utf32;

import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.expr.handler.ChannelContext;
import io.github.light0x00.letty.expr.handler.OutboundPipeline;
import io.github.light0x00.letty.expr.handler.adapter.ChannelHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/4
 */
@Slf4j
public class UTF32Encoder extends ChannelHandlerAdapter {

    @Override
    public void onWrite(ChannelContext context, Object msg, OutboundPipeline next) {
        String str = ((String) msg);

        int capacity = str.codePointCount(0, str.length()) * 4 + 4;

        RecyclableByteBuffer buf = context.allocateBuffer(capacity);

        str.codePoints().forEach(buf::putInt);

        buf.putInt("\n".codePointAt(0));

        log.info("write");

        context.channel().write(buf)
                .addListener(
                        f -> {
                            if (f.isSuccess()) {
                                log.info("actual write");
                            } else {
                                f.cause().printStackTrace();
                            }
                        }
                );
    }

}
