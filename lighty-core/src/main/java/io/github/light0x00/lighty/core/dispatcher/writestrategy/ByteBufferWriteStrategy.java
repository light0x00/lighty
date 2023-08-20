package io.github.light0x00.lighty.core.dispatcher.writestrategy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;

/**
 * @author light0x00
 * @since 2023/8/12
 */
public class ByteBufferWriteStrategy implements WriteStrategy {

    private final ByteBuffer buffer;

    public ByteBufferWriteStrategy(byte[] arr) {
        this.buffer = ByteBuffer.wrap(arr);
    }

    public ByteBufferWriteStrategy(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public long write(GatheringByteChannel channel) throws IOException {
        return channel.write(buffer);
    }

    @Override
    public long remaining() {
        return buffer.remaining();
    }

    @Override
    public Object getSource() {
        return buffer;
    }

    @Override
    public void close() {

    }
}
