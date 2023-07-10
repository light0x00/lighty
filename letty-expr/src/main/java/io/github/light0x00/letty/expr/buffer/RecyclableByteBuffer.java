package io.github.light0x00.letty.expr.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


public class RecyclableByteBuffer extends RingByteBuffer {

    protected final ByteBuffer bakingBuffer;

    private final BufferPool bufferPool;

    private final AtomicBoolean recycled = new AtomicBoolean();

    public RecyclableByteBuffer(ByteBuffer originalBuffer, int offset, int length, BufferPool bufferPool) {
        this(originalBuffer, originalBuffer.slice(offset, length), bufferPool);
    }

    public RecyclableByteBuffer(ByteBuffer originalBuffer, BufferPool bufferPool) {
        this(originalBuffer, originalBuffer, bufferPool);
    }

    public RecyclableByteBuffer(ByteBuffer originalBuffer, ByteBuffer sliceBuffer, BufferPool bufferPool) {
        super(sliceBuffer);
        this.bakingBuffer = originalBuffer;
        this.bufferPool = bufferPool;
    }

    public void release() {
        if (recycled.compareAndSet(false, true))
            bufferPool.recycle(bakingBuffer);
    }

//    @Override
//    protected void beforeAccessBuffer() {
//        if (recycled.get()) {
//            throw new IllegalStateException("Illegal access to buffer. The baking buffer has been recycled.");
//        }
//    }
}
