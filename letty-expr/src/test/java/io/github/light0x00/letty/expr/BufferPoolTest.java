package io.github.light0x00.letty.expr;


import io.github.light0x00.letty.expr.buffer.BufferPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class BufferPoolTest {

    /**
     * Test the idempotence when repeatedly recycle an identical ByteBuffer.
     */
    @Test
    public void testRecycleIdempotence() {
        AtomicInteger allocCount = new AtomicInteger();

        var bufferPool = new BufferPool<>((c) -> {
            allocCount.incrementAndGet();
            return ByteBuffer.allocate(c);
        });

        var buf = bufferPool.take(4);

        bufferPool.recycle(buf);
        bufferPool.recycle(buf);

        bufferPool.take(buf.capacity());
        bufferPool.take(buf.capacity());

        Assertions.assertEquals(2, allocCount.get());
    }

    /**
     * When there is already a larger (or equal) buffer in pool,
     * the taking operation should return a slice of that existing one.
     */
    @Test
    public void testSlice() {
        AtomicInteger allocCount = new AtomicInteger();

        var bufferPool = new BufferPool<>((c) -> {
            allocCount.incrementAndGet();
            return ByteBuffer.allocate(c);
        });

        var buf16 = bufferPool.take(16);
        bufferPool.recycle(buf16);

        var buf4 = bufferPool.take(4);

        Assertions.assertEquals(1, allocCount.get());
        Assertions.assertEquals(4, buf4.capacity());
    }

}
