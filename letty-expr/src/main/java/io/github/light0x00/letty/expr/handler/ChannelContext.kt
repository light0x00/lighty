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

    fun write(data: Any): ListenableFutureTask<Void>

    fun close(): ListenableFutureTask<Void>

    fun allocateBuffer(capacity: Int): RecyclableByteBuffer

    fun shutdownOutput(): ListenableFutureTask<Void>

    fun shutdownInput(): ListenableFutureTask<Void>
}