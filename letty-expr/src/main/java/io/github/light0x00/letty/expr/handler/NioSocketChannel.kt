package io.github.light0x00.letty.expr.handler

import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption

/**
 * @author light0x00
 * @since 2023/7/12
 */
interface NioSocketChannel {

    fun write(data: Any): ListenableFutureTask<Void>

    fun close(): ListenableFutureTask<Void>

    fun closeFuture(): ListenableFutureTask<Void>

    fun shutdownOutput(): ListenableFutureTask<Void>

    fun shutdownInput(): ListenableFutureTask<Void>

    fun localAddress(): SocketAddress

    fun remoteAddress(): SocketAddress

    fun <T> setOption(name: SocketOption<T>, v: T)
}