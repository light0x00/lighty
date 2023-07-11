package io.github.light0x00.letty.expr.handler

import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer
import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SocketChannel


/**
 * @author light0x00
 * @since 2023/6/28
 */
interface ChannelContext {

    abstract fun write(data: Any): ListenableFutureTask<Void>

    abstract fun close(): ListenableFutureTask<Void>

    abstract fun allocateBuffer(capacity: Int): RecyclableByteBuffer

    abstract fun shutdownOutput(): ListenableFutureTask<Void>
}