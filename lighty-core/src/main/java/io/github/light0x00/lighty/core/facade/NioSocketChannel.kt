package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption

/**
 * @author light0x00
 * @since 2023/7/12
 */
interface NioSocketChannel : NioChannel {

    fun remoteAddress(): SocketAddress

    fun write(data: Any): ListenableFutureTask<Void>

    fun connectedFuture(): ListenableFutureTask<Void>

    fun closeFuture(): ListenableFutureTask<Void>

    fun shutdownOutput(): ListenableFutureTask<Void>

    fun shutdownInput(): ListenableFutureTask<Void>

}