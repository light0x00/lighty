package io.github.light0x00.letty.core.handler

import io.github.light0x00.letty.core.eventloop.NioEventLoop
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

/**
 * @author light0x00
 * @since 2023/7/11
 */
class NioServerSocketChannel(
    private val channel: ServerSocketChannel,
    private val key: SelectionKey,
    private val eventLoop: NioEventLoop
) {

    fun close() {
        eventLoop.execute {
            key.cancel()
            channel.close()
        }
    }

    fun <T> setOption(name: SocketOption<T>, value: T) {
        channel.setOption(name, value)
    }

    fun getLocalAddress(): SocketAddress {
        return channel.localAddress
    }
}