package io.github.light0x00.lighty.core.facade

import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.ServerSocketChannel

/**
 * @author light0x00
 * @since 2023/7/11
 */
abstract class NioServerSocketChannel(private val channel: ServerSocketChannel) : NioChannel {

    override fun <T> getOption(name: SocketOption<T>): T {
        return channel.getOption(name)
    }

    override fun <T> setOption(name: SocketOption<T>, value: T) {
        channel.setOption(name, value)
    }

    override fun localAddress(): SocketAddress {
        return channel.localAddress
    }
}