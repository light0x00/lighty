package io.github.light0x00.letty.core

import io.github.light0x00.letty.core.buffer.BufferPool
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.letty.core.facade.ChannelInitializer
import io.github.light0x00.letty.core.handler.Acceptor
import io.github.light0x00.letty.core.handler.NioServerSocketChannel
import io.github.light0x00.letty.core.util.LettyException
import io.github.light0x00.letty.core.util.Loggable
import io.github.light0x00.letty.core.util.log
import java.net.SocketAddress
import java.net.StandardProtocolFamily
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

/**
 * @author light0x00
 * @since 2023/7/10
 */
class ServerBootstrap : AbstractBootstrap<ServerBootstrap>(), Loggable {

    private var acceptorGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null

    fun group(group: NioEventLoopGroup): ServerBootstrap {
        this.acceptorGroup = group
        this.workerGroup = group
        return this
    }

    fun group(acceptorGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup): ServerBootstrap {
        this.acceptorGroup = acceptorGroup
        this.workerGroup = workerGroup
        return this
    }

    fun bind(address: SocketAddress): ListenableFutureTask<NioServerSocketChannel> {
        if (acceptorGroup == null || workerGroup == null) {
            throw LettyException("group not set")
        }
        val configuration = buildConfiguration()
        return Server(acceptorGroup!!, workerGroup!!, configuration)
            .bind(address)
    }

    class Server(
        private val parent: NioEventLoopGroup,
        private val child: NioEventLoopGroup,
        private val configuration: LettyConfiguration
    ) : Loggable {
        fun bind(address: SocketAddress): ListenableFutureTask<NioServerSocketChannel> {
            val ssc = ServerSocketChannel.open(StandardProtocolFamily.INET)
            ssc.configureBlocking(false)
            val bindFuture = ListenableFutureTask<NioServerSocketChannel>(null)
            val eventLoop = parent.next()

            eventLoop.register(ssc, SelectionKey.OP_ACCEPT) { key ->
                Acceptor(ssc, key, child, configuration)
            }.addListener { futureTask ->
                ssc.bind(address)
                log.debug("Listen on {}", address)
                val key = futureTask.get()
                bindFuture.setSuccess(NioServerSocketChannel(ssc, key, eventLoop))
            }
            return bindFuture
        }
    }

}

