package io.github.light0x00.letty.expr.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecyclableByteBuffer<T extends ByteBuffer> extends ReadWriteByteBuffer<T> {

    protected final T bakingBuffer;

    private final BufferPool<T> bufferPool;

    private final AtomicBoolean recycled = new AtomicBoolean();

    @SuppressWarnings("unchecked")
    public RecyclableByteBuffer(T originalBuffer, int offset, int length, BufferPool<T> bufferPool) {
        super((T) originalBuffer.slice(offset, length));

        this.bakingBuffer = originalBuffer;
        this.bufferPool = bufferPool;
    }

    public RecyclableByteBuffer(T originalBuffer, BufferPool<T> bufferPool) {
        this(originalBuffer, 0, originalBuffer.capacity(), bufferPool);
    }

    public void release() {
        if (recycled.compareAndSet(false, true))
            bufferPool.recycle(bakingBuffer);
    }

    @Override
    protected void beforeAccessBuffer() {
        if (recycled.get()) {
            throw new IllegalStateException("Illegal access to buffer. The baking buffer has been recycled.");
        }
    }
}
