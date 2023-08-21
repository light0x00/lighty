package io.github.light0x00.lighty.codec;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.core.handler.ChannelContext;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

/**
 * @author light0x00
 * @since 2023/8/19
 */
public class StringDecoder extends InboundChannelHandlerAdapter {

    StatefulCharsetDecoder decoder;

    public StringDecoder(Charset charset) {
        decoder = new StatefulCharsetDecoder(charset);
    }

    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        try (var buf = (RecyclableBuffer) data) {
            String str = decoder.decode(buf.readableSlices());
            if (str.isEmpty()) {
                return;
            }
            pipeline.next(str)
                    .addListener(pipeline.upstreamFuture());
        }
    }
}
