package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import java.net.SocketAddress

/**
 * @author light0x00
 * @since 2023/7/12
 */
interface NioSocketChannel : NioChannel {

    fun remoteAddress(): SocketAddress

    /**
     * Write data via current handler through the subsequent handlers in [io.github.light0x00.lighty.core.handler.OutboundPipeline].
     * The data written through this call is not guaranteed to be written to the socket send buffer immediately.
     */
    fun write(data: Any): ListenableFutureTask<Void>

    /**
     * Request to flush all pending data, which means the data will be written to the socket send buffer.
     */
    fun flush()

    /**
     * Equivalent to call [write] and [flush].
     */
    fun writeAndFlush(data: Any): ListenableFutureTask<Void>

    fun connectedFuture(): ListenableFutureTask<Void>

    fun closedFuture(): ListenableFutureTask<Void>

    fun shutdownOutput(): ListenableFutureTask<Void>

    fun shutdownInput(): ListenableFutureTask<Void>

}