package io.github.light0x00.lighty.core.dispatcher.writestrategy;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.buffer.RingBuffer;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;

/**
 * @author light0x00
 * @since 2023/8/12
 */
@AllArgsConstructor
public class RingBufferWriteStrategy implements WriteStrategy {

    private RingBuffer ringBuffer;

    @Override
    public long write(GatheringByteChannel channel) throws IOException {
        return ringBuffer.writeToChannel(channel);
    }

    @Override
    public long remaining() {
        return ringBuffer.remainingCanGet();
    }

    @Override
    public Object getSource() {
        return ringBuffer;
    }

    @Override
    public void close() {
        if(ringBuffer instanceof RecyclableBuffer recyclable){
            recyclable.release();
        }
    }
}
