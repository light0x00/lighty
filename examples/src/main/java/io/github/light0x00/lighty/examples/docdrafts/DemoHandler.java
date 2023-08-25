package io.github.light0x00.lighty.examples.docdrafts;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.DuplexChannelHandlerAdapter;
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
        RecyclableBuffer buf = context.allocateBuffer(1024);
        //2.装入数据
        buf.put("Hello".getBytes());
        //3.写入
        context.write(buf);
    }

    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        try (var buf = (RecyclableBuffer) data) {
            //read out bytes in `buf` object
        }
    }
}
