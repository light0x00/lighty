package io.github.light0x00.letty.expr

import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer
import java.nio.ByteBuffer
import java.util.function.Function


/**
 * @author light0x00
 * @since 2023/6/28
 */
abstract class ChannelContext(
) {

    abstract fun write(data: Any): ListenableFutureTask<Void>

    abstract fun close(): ListenableFutureTask<Void>

    abstract fun allocateBuffer(capacity: Int): RecyclableByteBuffer
}