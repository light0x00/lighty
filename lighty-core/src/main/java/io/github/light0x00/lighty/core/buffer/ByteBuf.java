package io.github.light0x00.lighty.core.buffer;

import io.github.light0x00.lighty.core.facade.LightyException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class ByteBuf extends RingBuffer implements Closeable {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    BufferPool pool;
    volatile boolean hasReleased = false;
    final ByteBuffer backingBuffer;

    public ByteBuf(BufferPool pool, ByteBuffer originalBuffer, int offset, int length) {
        super(offset == 0 && length == originalBuffer.capacity() ? originalBuffer : originalBuffer.slice(offset, length));
        this.pool = pool;
        this.backingBuffer = originalBuffer;
    }

    private void ensureNotReleased() {
        if (hasReleased) {
            throw new LightyException("The buffer has already released, {}", this.toString());
        }
    }

    /**
     * Mark as released, return true only for the first thread‘s invocation.
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
            return super.writePosition();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int readPosition() {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.readPosition();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int capacity() {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.capacity();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public byte get() {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.get();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf get(byte[] dst) {
        try {
            readLock.lock();
            ensureNotReleased();
            super.get(dst);
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
            super.get(dst, offset, length);
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
            super.put(value);
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
            super.put(src);
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
            super.put(src, offset, length);
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
            super.put(src);
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
            super.put(src, length);
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
            super.put(src, offset, length);
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
            super.put(src);
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
            super.put(src, length);
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
            return super.readFromChannel(channel);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int writeToChannel(GatheringByteChannel channel) throws IOException {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.writeToChannel(channel);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getInt() {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.getInt();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ByteBuf putInt(int value) {
        try {
            readLock.lock();
            ensureNotReleased();
            super.putInt(value);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int remainingCanGet() {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.remainingCanGet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int remainingCanPut() {
        try {
            readLock.lock();
            ensureNotReleased();
            return super.remainingCanPut();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void clear() {
        try {
            readLock.lock();
            ensureNotReleased();
            super.clear();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void close() {
        release();
    }
}
