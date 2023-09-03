package io.github.light0x00.lighty.examples.docdrafts;

import io.github.light0x00.lighty.core.buffer.ByteBuf;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/8/16
 */
public class DemoHandler extends InboundChannelHandlerAdapter {

    @Override
    public void onConnected(@Nonnull ChannelContext context) {
        //1.申请 buffer
        ByteBuf buf = context.allocateBuffer(1024);
        //2.装入数据
        buf.put("Hello".getBytes());
        //3.写入
        context.write(buf);
    }

    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        try (var buf = (ByteBuf) data) {
            //read out bytes in `buf` object
        }
    }
}
