package io.github.light0x00.letty.core

import io.github.light0x00.letty.core.buffer.BufferPool
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.letty.core.handler.SocketChannelEventHandler
import io.github.light0x00.letty.core.handler.NioSocketChannel
import io.github.light0x00.letty.core.util.LettyException
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.function.Function

/**
 * @author light0x00
 * @since 2023/7/31
 */
class ClientBootstrap : AbstractBootstrap() {

    private var group: NioEventLoopGroup? = null
    private var handlerConfigurer: ChannelHandlerConfigurer? = null
    private var properties: LettyProperties? = null
    private var bufferPool: BufferPool? = null

    fun group(group: NioEventLoopGroup): ClientBootstrap {
        this.group = group
        return this
    }

    fun handlerConfigurer(handlerConfigurer: ChannelHandlerConfigurer): ClientBootstrap {
        this.handlerConfigurer = handlerConfigurer;
        return this
    }

    fun properties(properties: LettyProperties): ClientBootstrap {
        this.properties = properties
        return this
    }

    fun bufferPool(pool: BufferPool): ClientBootstrap {
        this.bufferPool = pool;
        return this
    }

    fun connect(address: SocketAddress): ListenableFutureTask<NioSocketChannel> {

        if (group == null) {
            throw LettyException("group not set")
        }
        if (properties == null) {
            properties = defaultProperties
        }
        if (bufferPool == null) {
            bufferPool = defaultBufferPool
        }
        if (handlerConfigurer == null) {
            throw LettyException("handlerConfigurer not set")
        }

        return Client(group!!, buildConfiguration(properties!!, bufferPool!!, handlerConfigurer!!)).connect(address)
    }

    class Client(private val group: NioEventLoopGroup, private var configuration: LettyConfiguration) {

        fun connect(address: SocketAddress?): ListenableFutureTask<NioSocketChannel> {
            val channel = SocketChannel.open()
            channel.configureBlocking(false)
            val connectableFuture = ListenableFutureTask<NioSocketChannel>(null)
            val eventLoop = group.next()
            eventLoop.register(channel, SelectionKey.OP_CONNECT,
                Function { key: SelectionKey? ->
                    SocketChannelEventHandler(
                        eventLoop, channel, key,
                        configuration, connectableFuture
                    )
                })
                .addListener {
                    // attach context before connect , so that to ensure the context not null when the event triggered.
                    // connect 动作应发生在 attach context 之后, 这样才能保证事件触发时能拿到非空的 context
                    channel.connect(address)
                }
            return connectableFuture
        }
    }

}



