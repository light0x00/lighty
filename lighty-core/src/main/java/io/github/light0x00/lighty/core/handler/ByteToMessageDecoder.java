package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.buffer.RingBuffer;
import io.github.light0x00.lighty.core.facade.LightyException;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public abstract class ByteToMessageDecoder extends InboundChannelHandlerAdapter {

    private RecyclableBuffer decodeBuf;

    private final int bufSize;

    public ByteToMessageDecoder(int bufSize) {
        this.bufSize = bufSize;
    }

    @Override
    public void onConnected(ChannelContext context) {
        decodeBuf = context.allocateBuffer(bufSize);
    }

    @Override
    public void onClosed(ChannelContext context) {
        decodeBuf.release();
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        try (RecyclableBuffer srcBuf = (RecyclableBuffer) data) {

            while (srcBuf.remainingCanGet() > 0) {
                decodeBuf.put(srcBuf, Math.min(decodeBuf.remainingCanPut(), srcBuf.remainingCanGet()));

                decode(context, decodeBuf, next);

                if (decodeBuf.remainingCanPut() == 0) {
                    throw new LightyException("The decode buffer is already full, but the decoder has not yet read it. ");
                }
            }

        }
    }

    protected abstract void decode(ChannelContext context, RingBuffer data, InboundPipeline next);

}
