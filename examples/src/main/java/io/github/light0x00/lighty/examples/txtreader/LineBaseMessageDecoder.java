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
        Delimiter delimiter;
        while (offset < str.length()) {
            if ((delimiter = findDelimiter(str, offset)).index < 0) {
                messageBuilder.append(str);
                return;
            }
            messageBuilder.append(str, offset, delimiter.index);
            pipeline.next(unescape(messageBuilder.toString()));
            messageBuilder.setLength(0);
            offset = delimiter.index + delimiter.length;
        }
    }

    private static Delimiter findDelimiter(String str, int offset) {
        int idx;
        if ((idx = str.indexOf("\r\n", offset)) > -1) {
            return new Delimiter(idx, 2);
        } else if ((idx = str.indexOf("\n", offset)) > -1) {
            return new Delimiter(idx, 1);
        } else {
            return new Delimiter(-1, 0);
        }
    }

    record Delimiter(int index, int length) {

    }

    private static String unescape(String str) {
        return str.replace("\\\\", "\\")
                .replace("\\n", "\n");
    }
}
