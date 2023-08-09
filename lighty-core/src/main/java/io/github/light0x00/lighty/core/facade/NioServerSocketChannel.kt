package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.NioEventLoop
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

/**
 * @author light0x00
 * @since 2023/7/11
 */
abstract class NioServerSocketChannel(
    private val channel: ServerSocketChannel,
    private val key: SelectionKey,
    private val eventLoop: NioEventLoop
) {

    abstract fun close(): ListenableFutureTask<Void>

    fun <T> setOption(name: SocketOption<T>, value: T) {
        channel.setOption(name, value)
    }

    fun getLocalAddress(): SocketAddress {
        return channel.localAddress
    }
}