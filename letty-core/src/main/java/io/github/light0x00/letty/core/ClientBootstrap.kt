package io.github.light0x00.letty.core

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.letty.core.handler.NioSocketChannel
import io.github.light0x00.letty.core.handler.SocketChannelEventHandler
import io.github.light0x00.letty.core.util.LettyException
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.function.Function

/**
 * @author light0x00
 * @since 2023/7/31
 */
class ClientBootstrap : AbstractBootstrap<ClientBootstrap>() {

    private var group: NioEventLoopGroup? = null

    fun group(group: NioEventLoopGroup): ClientBootstrap {
        this.group = group
        return this
    }

    fun connect(address: SocketAddress): ListenableFutureTask<NioSocketChannel> {

        if (group == null) {
            throw LettyException("group not set")
        }
        return Client(group!!, buildConfiguration()).connect(address)
    }

    private class Client(private val group: NioEventLoopGroup, private var configuration: LettyConfiguration) {

        fun connect(address: SocketAddress?): ListenableFutureTask<NioSocketChannel> {
            val channel = SocketChannel.open()
            channel.configureBlocking(false)
            val connectableFuture = ListenableFutureTask<NioSocketChannel>(null)
            val eventLoop = group.next()
            eventLoop.register(channel, SelectionKey.OP_CONNECT, Function { key: SelectionKey? ->
                SocketChannelEventHandler(
                    eventLoop, channel, key, configuration, connectableFuture
                )
            }).addListener {
                    // attach context before connect , so that to ensure the context not null when the event triggered.
                    // connect 动作应发生在 attach context 之后, 这样才能保证事件触发时能拿到非空的 context
                    channel.connect(address)
                }
            return connectableFuture
        }
    }

}



