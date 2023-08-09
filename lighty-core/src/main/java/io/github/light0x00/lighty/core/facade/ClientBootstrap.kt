package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.dispatcher.SocketChannelEventHandler
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

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
            throw LightyException("group not set")
        }
        return Client(group!!, buildConfiguration()).connect(address)
    }

    private class Client(private val group: NioEventLoopGroup, private var configuration: LightyConfiguration) {

        fun connect(address: SocketAddress?): ListenableFutureTask<NioSocketChannel> {
            val channel = SocketChannel.open()
            channel.configureBlocking(false)
            val connectableFuture = ListenableFutureTask<NioSocketChannel>(null)
            val eventLoop = group.next()
            eventLoop
                .register(channel, SelectionKey.OP_CONNECT) { key: SelectionKey ->
                    /*
                    需注意, connect 操作 与 bind 操作的不同, 前者是需要产生网络数据包收发的, 而后者只是向操作系统申请资源
                    这决定了, 非阻塞模式下, connect 操作需要分两步:
                    1. 3-way-handshake (对应 SocketChannel#connect, 此时并不会产生成功/失败的结果)
                    2. select 返回 (对应 SocketChannel#finishConnect, 此时才能查询到结果, 异常会直接抛出)

                    而与之不同, bind 操作的结果是立即返回的, 如果有异常会直接抛出
                    这里差异决定了这里为什么没有 try/catch connect 异常, 因为被放入了 connectable 事件的处理方法中.

                    另外 connect 操作放在这里, 可确保 register 、connect 两个操作的原子性:
                    1. 只有在 register 成功时才会执行 connect
                    2. 如果 connect 失败, 则需要 deregister
                     */
                    channel.connect(address)
                    SocketChannelEventHandler(
                        eventLoop,
                        channel,
                        key,
                        configuration,
                        connectableFuture
                    )
                }
            return connectableFuture
        }
    }

}



