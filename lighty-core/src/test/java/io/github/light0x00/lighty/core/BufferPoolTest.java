package io.github.light0x00.lighty.core;


import io.github.light0x00.lighty.core.buffer.DefaultBufferPool;
import io.github.light0x00.lighty.core.facade.LightyException;
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

        var bufferPool = new DefaultBufferPool((c) -> {
            allocCount.incrementAndGet();
            return ByteBuffer.allocate(c);
        });

        var buf = bufferPool.take(4);

        buf.release();
        buf.release();

        bufferPool.take(4);
        bufferPool.take(4);


        Assertions.assertEquals(2, allocCount.get());
        Assertions.assertThrows(LightyException.class, () -> buf.get());
    }

    /**
     * When there is already a larger (or equal) buffer in pool,
     * the taking operation should return a slice of that existing one.
     */
    @Test
    public void testSlice() {
        AtomicInteger allocCount = new AtomicInteger();

        var bufferPool = new DefaultBufferPool((c) -> {
            allocCount.incrementAndGet();
            return ByteBuffer.allocate(c);
        });

        var buf16 = bufferPool.take(16);
        buf16.release();

        var buf4 = bufferPool.take(4);

        Assertions.assertEquals(1, allocCount.get());
        Assertions.assertEquals(4, buf4.capacity());
    }

}
