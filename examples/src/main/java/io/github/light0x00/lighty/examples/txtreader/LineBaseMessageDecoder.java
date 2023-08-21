package io.github.light0x00.lighty.examples.txtreader;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

import javax.annotation.Nonnull;

/**
 * Decode message base on '\n' delimiter character
 *
 * @author light0x00
 * @since 2023/8/18
 */
public class LineBaseMessageDecoder extends InboundChannelHandlerAdapter {

    StringBuilder messageBuilder = new StringBuilder();

    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        String str = (String) data;

        int offset = 0;
        int delimiterIdx;
        while (offset < str.length()) {
            if ((delimiterIdx = str.indexOf("\n", offset)) < 0) {
                messageBuilder.append(str);
                return;
            }
            if (delimiterIdx >= offset) {
                messageBuilder.append(str, offset, delimiterIdx);
            }
            pipeline.next(unescape(messageBuilder.toString()));
            messageBuilder.setLength(0);
            offset = delimiterIdx + 1;
        }
    }

    private static String unescape(String str) {
        return str.replace("\\\\", "\\")
                .replace("\\n", "\n");
    }
}
