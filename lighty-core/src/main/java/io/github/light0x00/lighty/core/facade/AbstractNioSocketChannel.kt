package io.github.light0x00.lighty.core.facade

import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SocketChannel

/**
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