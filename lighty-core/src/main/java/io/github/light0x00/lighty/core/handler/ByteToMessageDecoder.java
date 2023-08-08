package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.buffer.RingBuffer;
import io.github.light0x00.lighty.core.handler.adapter.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.util.LettyException;

import java.nio.ByteBuffer;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ByteToMessageDecoder extends InboundChannelHandlerAdapter {

    private final RingBuffer decodeBuf;

    public ByteToMessageDecoder(int bufSize) {
        decodeBuf = new RingBuffer(ByteBuffer.allocate(bufSize));
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        RecyclableBuffer srcBuf = (RecyclableBuffer) data;

        while (srcBuf.remainingCanGet() > 0) {
            decodeBuf.put(srcBuf, Math.min(decodeBuf.remainingCanPut(), srcBuf.remainingCanGet()));

            decode(context, decodeBuf, next);

            if (decodeBuf.remainingCanPut() == 0) {
                throw new LettyException("The decode buffer is full, but the decoder has not yet read it. ");
            }
        }
    }

    protected abstract void decode(ChannelContext context, RingBuffer data, InboundPipeline next);

}
