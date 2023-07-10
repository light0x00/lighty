package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import io.github.light0x00.letty.expr.buffer.RingByteBuffer;
import io.github.light0x00.letty.expr.handler.ChannelContext;
import io.github.light0x00.letty.expr.handler.InboundPipeline;
import io.github.light0x00.letty.expr.handler.adapter.InboundChannelHandlerAdapter;

import java.nio.ByteBuffer;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ByteToMessageDecoder extends InboundChannelHandlerAdapter {

    private final RingByteBuffer decodeBuf;

    public ByteToMessageDecoder(int bufSize) {
        decodeBuf = new RingByteBuffer(ByteBuffer.allocate(bufSize));
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        RecyclableByteBuffer srcBuf = (RecyclableByteBuffer) data;

        while (srcBuf.remainingCanGet() > 0) {
            decodeBuf.put(srcBuf, Math.min(decodeBuf.remainingCanPut(), srcBuf.remainingCanGet()));

            decode(context, decodeBuf, next);

            if (decodeBuf.remainingCanPut() == 0) {
                throw new IllegalStateException("The decode buffer is full, but the decoder not read.");
            }
        }
    }

    protected abstract void decode(ChannelContext context, RingByteBuffer data, InboundPipeline next);

}
