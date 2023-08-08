package io.github.light0x00.lighty.core

import io.github.light0x00.lighty.core.handler.NioSocketChannel
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SocketChannel

/**
 * The functionality of this channel decorator is limited.
 * Usually due to the underlying channel has not been fully constructed.
 * eg: The tha channel may be registered to a selector but not yet connected.
 * So that the read/write functionality is not available.
 *
 * @author light0x00
 * @since 2023/7/31
 */
abstract class AbstractNioSocketChannel(private val javaChannel: SocketChannel) : NioSocketChannel {

    override fun localAddress(): SocketAddress {
        return javaChannel.localAddress
    }

    override fun remoteAddress(): SocketAddress = javaChannel.remoteAddress

    override fun <T> setOption(name: SocketOption<T>, v: T) {
        javaChannel.setOption(name, v)
    }

    override fun <T> getOption(name: SocketOption<T>): T = javaChannel.getOption(name)
}