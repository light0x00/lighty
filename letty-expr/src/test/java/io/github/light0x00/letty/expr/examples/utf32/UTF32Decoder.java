package io.github.light0x00.letty.expr.examples.utf32;

import io.github.light0x00.letty.expr.handler.ByteToMessageDecoder;
import io.github.light0x00.letty.expr.buffer.RingByteBuffer;
import io.github.light0x00.letty.expr.handler.ChannelContext;
import io.github.light0x00.letty.expr.handler.InboundPipeline;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public class UTF32Decoder extends ByteToMessageDecoder {

    StringBuilder sb = new StringBuilder();

    public UTF32Decoder() {
        super(4);
    }

    @Override
    protected void decode(ChannelContext context, RingByteBuffer data, InboundPipeline next) {
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
