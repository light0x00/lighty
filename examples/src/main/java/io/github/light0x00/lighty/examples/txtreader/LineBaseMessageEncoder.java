package io.github.light0x00.lighty.examples.txtreader;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.OutboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;

import javax.annotation.Nonnull;

/**
 * Encode message base on '\n' delimiter character
 *
 * @author light0x00
 * @since 2023/8/18
 */
public class LineBaseMessageEncoder extends OutboundChannelHandlerAdapter {

    @Override
    public void onWrite(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull OutboundPipeline pipeline) {
        String message = (String) data;

        // "\\n\n" -> "\\\\n\\n"
        String encoded = message.replace("\\", "\\\\")
                .replace("\n", "\\n");

        context.write(encoded);

        pipeline.next("\n")
                .addListener(pipeline.upstreamFuture());
    }
}
