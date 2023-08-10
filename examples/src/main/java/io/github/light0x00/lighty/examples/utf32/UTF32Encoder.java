package io.github.light0x00.lighty.examples.utf32;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.OutboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/4
 */
@Slf4j
public class UTF32Encoder extends OutboundChannelHandlerAdapter {

    static final int MESSAGE_DELIMITER = "\n".codePointAt(0);

    @Override
    public void onWrite(ChannelContext context, Object msg, OutboundPipeline next) {
        String str = ((String) msg);

        int capacity = str.codePointCount(0, str.length()) * 4 + 4;
        RecyclableBuffer buf = context.allocateBuffer(capacity);

        str.codePoints().forEach(buf::putInt);
        buf.putInt(MESSAGE_DELIMITER);

        next.invoke(buf)
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
