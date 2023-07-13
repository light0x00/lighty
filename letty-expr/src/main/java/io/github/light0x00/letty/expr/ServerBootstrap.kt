package io.github.light0x00.letty.expr

import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup

/**
 * @author light0x00
 * @since 2023/7/10
 */
class ServerBootstrap {

    private lateinit var channelInitializer: ChannelConfigurationProvider
    private lateinit var acceptorGroup: NioEventLoopGroup
    private lateinit var workerGroup: NioEventLoopGroup

    fun group(acceptorGroup: NioEventLoopGroup): ServerBootstrap {
        this.acceptorGroup = acceptorGroup
        return this
    }

    fun group(acceptorGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup): ServerBootstrap {
        this.acceptorGroup = acceptorGroup
        this.workerGroup = workerGroup
        return this
    }

    fun channelInitializer(): ServerBootstrap {
        return this
    }

    fun buildServer(): Server {
        return Server(acceptorGroup, workerGroup, channelInitializer)
    }
}