package io.github.light0x00.lighty.codec;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.buffer.RingBuffer;
import io.github.light0x00.lighty.core.facade.LightyException;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

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
    public void onInitialize(ChannelContext context) {
        decodeBuf = context.allocateBuffer(bufSize);
    }

    @Override
    public void onDestroy(ChannelContext context) {
        decodeBuf.release();
    }

    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline pipeline) {
        try (RecyclableBuffer srcBuf = (RecyclableBuffer) data) {

            while (srcBuf.remainingCanGet() > 0) {
                decodeBuf.put(srcBuf, Math.min(decodeBuf.remainingCanPut(), srcBuf.remainingCanGet()));
                decode(context, decodeBuf, pipeline);

                if (decodeBuf.remainingCanPut() == 0) {
                    throw new LightyException("The decode buffer is already full, but the decoder has not yet read it. ");
                }
            }

        }
    }

    /**
     * Each time the readable event triggered, this method will be invoked, passing the accumulated bytes.
     * A point need to aware is that the accumulated bytes are not necessary to reach the number specified.
     * In other words, the bytes passed to this method, may not be full.
     *
     * @param bytes the bytes accumulated
     */
    protected abstract void decode(ChannelContext context, RingBuffer bytes, InboundPipeline pipeline);

}
