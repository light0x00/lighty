package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.handler.ByteToMessageDecoder;
import io.github.light0x00.letty.core.buffer.RingBuffer;
import io.github.light0x00.letty.core.handler.ChannelContext;
import io.github.light0x00.letty.core.handler.InboundPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/4
 */
@Slf4j
public class UTF32Decoder extends ByteToMessageDecoder {

    StringBuilder sb = new StringBuilder();

    public UTF32Decoder() {
        super(4);
    }

    @Override
    protected void decode(ChannelContext context, RingBuffer data, InboundPipeline next) {
        log.info("decode..");
        while (data.remainingCanGet() >= 4) {
            String ch = Character.toString(data.getInt());
            if (ch.equals("\n")) {
                next.invoke(sb.toString());
            } else {
                sb.append(ch);
            }
        }
    }
}
