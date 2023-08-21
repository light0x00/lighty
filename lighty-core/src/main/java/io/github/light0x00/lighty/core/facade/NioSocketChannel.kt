package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.nio.channels.FileChannel

/**
 * @author light0x00
 * @since 2023/7/12
 */
interface NioSocketChannel : NioChannel {

    /**
     * @see [java.nio.channels.SocketChannel.getRemoteAddress]
     */
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

    /**
     * Send file in a zero-copy way.
     */
    fun transfer(fc: FileChannel): ListenableFutureTask<Void>

    /**
     * Equivalent to call [transfer] and [flush].
     */
    fun transferAndFlush(fc: FileChannel): ListenableFutureTask<Void>

    /**
     * A future that will get notified when the channel get connected
     */
    fun connectedFuture(): ListenableFutureTask<Void>

    /**
     * A future that will get notified when the channel closed
     */
    fun closedFuture(): ListenableFutureTask<Void>

    /**
     * Shutdown the output side of the channel
     */
    fun shutdownOutput(): ListenableFutureTask<Void>

    /**
     * Shutdown the input side of the channel, which means read completed.
     */
    fun shutdownInput(): ListenableFutureTask<Void>

}