package io.github.light0x00.letty.core.handler

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SocketChannel

/**
 * @author light0x00
 * @since 2023/7/9
 */
abstract class AbstractNioSocketChannelImpl(private val javaChannel: SocketChannel) : NioSocketChannel {

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