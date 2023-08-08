package io.github.light0x00.lighty.core.handler

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

    abstract fun close() :ListenableFutureTask<Void>
//        //TODO 调研 当 ServerSocketChannel cancel 后, 其 accept 的 SocketChannel 以及 SelectionKey 的状态, 考虑是否需要释放资源 [issue0001]
//        eventLoop.execute {
//            key.cancel()
//            channel.close()
//        }

    fun <T> setOption(name: SocketOption<T>, value: T) {
        channel.setOption(name, value)
    }

    fun getLocalAddress(): SocketAddress {
        return channel.localAddress
    }
}