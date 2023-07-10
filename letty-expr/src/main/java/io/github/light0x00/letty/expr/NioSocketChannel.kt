package io.github.light0x00.letty.expr

import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SocketChannel

/**
 * @author light0x00
 * @since 2023/7/9
 */
class NioSocketChannel(private val channel: SocketChannel) {

    fun localAddress(): SocketAddress {
        return channel.localAddress
    }
    fun remoteAddress(): SocketAddress {
        return channel.remoteAddress
    }

    fun <T> setOption(name: SocketOption<T>, v: T) {
        channel.setOption(name, v)
    }
}