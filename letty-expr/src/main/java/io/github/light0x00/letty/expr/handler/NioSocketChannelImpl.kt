package io.github.light0x00.letty.expr.handler

import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SocketChannel

/**
 * @author light0x00
 * @since 2023/7/9
 */
abstract class NioSocketChannelImpl(private val javaChannel: SocketChannel) : NioSocketChannel {

    abstract override fun write(data: Any): ListenableFutureTask<Void>

    abstract override fun close(): ListenableFutureTask<Void>

    abstract override fun shutdownOutput(): ListenableFutureTask<Void>

    abstract override fun shutdownInput(): ListenableFutureTask<Void>

    override fun localAddress(): SocketAddress {
        return javaChannel.localAddress
    }

    override fun remoteAddress(): SocketAddress {
        return javaChannel.remoteAddress
    }

    override fun <T> setOption(name: SocketOption<T>, v: T) {
        javaChannel.setOption(name, v)
    }
}