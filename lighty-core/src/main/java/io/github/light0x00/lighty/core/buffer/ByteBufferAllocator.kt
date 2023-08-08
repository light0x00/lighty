package io.github.light0x00.lighty.core.buffer

import java.nio.ByteBuffer

/**
 * @author light0x00
 * @since 2023/7/31
 */
interface ByteBufferAllocator {
    fun allocate(capacity: Int) : ByteBuffer
}