package io.github.light0x00.lighty.examples.utf32;

import io.github.light0x00.lighty.core.buffer.RingBuffer;
import io.github.light0x00.lighty.core.handler.ByteToMessageDecoder;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * @author light0x00
 * @since 2023/7/4
 */
@Slf4j
public class UTF32Decoder extends ByteToMessageDecoder {

    StringBuilder line = new StringBuilder();

    static final int MESSAGE_DELIMITER = '\n';

    public UTF32Decoder() {
        super(4);
    }

    @Override
    protected void decode(ChannelContext context, RingBuffer data, InboundPipeline next) {
        while (data.remainingCanGet() >= 4) {
            int cp = data.getInt();
            if (cp == MESSAGE_DELIMITER) {
                log.info("Decode a complete message");
                next.invoke(line.toString());
                line.setLength(0);
            } else {
                line.append(Character.toString(cp));
                log.info("Accumulate {} bytes", 4);
            }
        }
    }
}
