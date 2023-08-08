package io.github.light0x00.lighty.core.buffer

import java.nio.ByteBuffer

/**
 * @author light0x00
 * @since 2023/7/31
 */
class DefaultByteBufferAllocator(private val threshold: Int = 1024) : ByteBufferAllocator {
    override fun allocate(capacity: Int): ByteBuffer {
        return if (capacity < threshold) {
            ByteBuffer.allocate(capacity);
        } else {
            ByteBuffer.allocateDirect(capacity)
        }
    }

}