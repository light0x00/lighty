package io.github.light0x00.lighty.core.buffer;

import io.github.light0x00.lighty.core.facade.LightyException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class ByteBuf implements RingBuffer, Closeable {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    BufferPool pool;
    volatile boolean hasReleased = false;
    final ByteBuffer backingBuffer;

    RingBufferImpl ringBuffer;

    public ByteBuf(BufferPool pool, ByteBuffer originalBuffer, int offset, int length) {
        ringBuffer = new RingBufferImpl(
                offset == 0 && length == originalBuffer.capacity() ? originalBuffer : originalBuffer.slice(offset, length));
        this.pool = pool;
        this.backingBuffer = originalBuffer;
    }

    private void ensureNotReleased() {
        if (hasReleased) {
            throw new LightyException("The buffer has already released, {}", this.toString());
        }
    }

    /**
     * Mark as released, return true only for the first thread's invocation.
     */
    boolean markReleased() {
        try {
            writeLock.lock();
            if (hasReleased) {
                return false;
            }
            hasReleased = true;
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    public void release() {
        if (markReleased())
            pool.recycle(this);
    }

    @Override
    public int writePosition() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.writePosition();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int readPosition() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.readPosition();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int capacity() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.capacity();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public byte get() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.get();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf get(byte[] dst) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.get(dst);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf get(byte[] dst, int offset, int length) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.get(dst, offset, length);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(byte value) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(value);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(byte[] src) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(byte[] src, int offset, int length) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src, offset, length);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(ByteBuffer src) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(ByteBuffer src, int length) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src, length);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(ByteBuffer src, int offset, int length) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src, offset, length);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(RingBuffer src) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf put(RingBuffer src, int length) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.put(src, length);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int readFromChannel(ScatteringByteChannel channel) throws IOException {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.readFromChannel(channel);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int writeToChannel(GatheringByteChannel channel) throws IOException {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.writeToChannel(channel);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getInt() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.getInt();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf putInt(int value) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.putInt(value);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getLong() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.getLong();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf putLong(long value) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.putLong(value);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuffer[] readableSlices() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.readableSlices();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void moveReadPosition(int step) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.moveReadPosition(step);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void moveWritePosition(int step) {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.moveWritePosition(step);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int remainingCanGet() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.remainingCanGet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int remainingCanPut() {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.remainingCanPut();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getString(Charset charset) {
        try {
            readLock.lock();
            ensureNotReleased();
            return ringBuffer.getString(charset);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void clear() {
        try {
            readLock.lock();
            ensureNotReleased();
            ringBuffer.clear();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void close() {
        release();
    }
}
