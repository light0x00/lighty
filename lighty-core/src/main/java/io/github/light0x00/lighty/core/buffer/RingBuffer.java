package io.github.light0x00.lighty.core.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

/**
 * @author light0x00
 * @since 2023/8/27
 */
public interface RingBuffer {
    byte get();

    RingBuffer get(byte[] dst);

    RingBuffer get(byte[] dst, int offset, int length);

    RingBuffer put(byte value);

    RingBuffer put(byte[] src);

    RingBuffer put(byte[] src, int offset, int length);

    RingBuffer put(ByteBuffer src);

    RingBuffer put(ByteBuffer src, int length);

    RingBuffer put(ByteBuffer src, int offset, int length);

    RingBuffer put(RingBuffer src);

    RingBuffer put(RingBuffer src, int length);

    int readFromChannel(ScatteringByteChannel channel) throws IOException;

    int writeToChannel(GatheringByteChannel channel) throws IOException;

    int getInt();

    RingBuffer putInt(int value);

    long getLong();

    RingBuffer putLong(long value);

    String getString(Charset charset);

    int remainingCanGet();

    int remainingCanPut();

    ByteBuffer[] readableSlices();

    void moveReadPosition(int step);

    void moveWritePosition(int step);

    int writePosition();

    int readPosition();

    int capacity();

    void clear();
}
