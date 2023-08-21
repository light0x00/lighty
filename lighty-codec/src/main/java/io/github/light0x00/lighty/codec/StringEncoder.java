package io.github.light0x00.lighty.codec;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.OutboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

/**
 * @author light0x00
 * @since 2023/8/20
 */
public class StringEncoder extends OutboundChannelHandlerAdapter {

    Charset charset;

    public StringEncoder(Charset charset) {
        this.charset = charset;
    }

    @Override
    public void onWrite(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull OutboundPipeline pipeline) {
        String str = (String) data;
        pipeline.next(str.getBytes(charset))
                .addListener(pipeline.upstreamFuture());
    }
}
