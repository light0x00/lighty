package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.dispatcher.Acceptor
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.lighty.core.util.Loggable
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
            throw LightyException("group not set")
        }
        val configuration = buildConfiguration()
        return Server(acceptorGroup!!, workerGroup!!, configuration)
            .bind(address)
    }

    class Server(
        private val parent: NioEventLoopGroup,
        private val child: NioEventLoopGroup,
        private val configuration: LightyConfiguration
    ) : Loggable {
        fun bind(address: SocketAddress): ListenableFutureTask<NioServerSocketChannel> {
            val serverChannel = ServerSocketChannel.open(StandardProtocolFamily.INET)
            serverChannel.configureBlocking(false)
            val bindFuture = ListenableFutureTask<NioServerSocketChannel>(null)
            val eventLoop = parent.next()

            eventLoop
                .register(serverChannel, SelectionKey.OP_ACCEPT) { key ->
                    Acceptor(serverChannel, key, eventLoop, child, configuration, bindFuture)
                }
                .addListener {
                    if (it.isSuccess) {
                        val acceptor = it.get()
                        acceptor.bind(address)
                    } else {
                        //register 阶段失败, 关闭 serverChannel, 避免其一直处于 unbound 状态, 占用系统资源.
                        serverChannel.close()
                        bindFuture.setFailure(it.cause())
                    }
                }

            return bindFuture
        }
    }

}

