package io.github.light0x00.lighty.core.dispatcher

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.NioEventHandler
import io.github.light0x00.lighty.core.eventloop.NioEventLoop
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.lighty.core.facade.ChannelInitializer
import io.github.light0x00.lighty.core.facade.LightyConfiguration
import io.github.light0x00.lighty.core.facade.NioServerSocketChannel
import io.github.light0x00.lighty.core.util.Loggable
import io.github.light0x00.lighty.core.util.log
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

/**
 * @author light0x00
 * @since 2023/7/7
 */
class Acceptor(
    private val javaChannel: ServerSocketChannel,
    private val key: SelectionKey,
    private val eventLoop: NioEventLoop,
    private val workerGroup: NioEventLoopGroup,
    private val lightyConfiguration: LightyConfiguration,
    initializer: ChannelInitializer<NioServerSocketChannel>,
    private val bindFuture: ListenableFutureTask<NioServerSocketChannel>
) : NioEventHandler, Loggable {

    val channel: NioServerSocketChannel
    private var closed = false
    private val closedFuture: ListenableFutureTask<Void> = ListenableFutureTask(null)

    init {
        channel = object : NioServerSocketChannel(javaChannel) {
            override fun close(): ListenableFutureTask<Void> {
                return this@Acceptor.close()
            }
        }

        initializer.initChannel(channel)
    }

    override fun onEvent(key: SelectionKey) {
        val incomingChannel = (key.channel() as ServerSocketChannel).accept()
        incomingChannel.configureBlocking(false)

        val workerEventLoop = workerGroup.next()

        workerEventLoop
            .register(incomingChannel, SelectionKey.OP_READ) { selectionKey: SelectionKey? ->
                object :
                    SocketChannelEventHandler(workerEventLoop, incomingChannel, selectionKey, lightyConfiguration) {
                    // 对于 server 侧的 SocketChannel 而言, 其 connected 事件, 在 ServerSocketChannel acceptable 时就触发
                    init {
                        this.dispatcher.onConnected()
                        this.connectableFuture.setSuccess(this.channel)
                    }
                }
            }
    }

    override fun close(): ListenableFutureTask<Void> {
        eventLoop.execute { close0() }
        return closedFuture
    }

    fun bind(address: SocketAddress): ListenableFutureTask<NioServerSocketChannel> {
        eventLoop.execute { bind0(address) }
        return bindFuture
    }

    private fun bind0(address: SocketAddress) {
        try {
            javaChannel.bind(address)
        } catch (cause: Throwable) {
            //bind 失败时, 需要做两件事:
            // 1. key.cancel() , 避免其留在 selector 中
            // 2. channel.close(), 使之从状态 unbound 变为 closed
            close0()
            bindFuture.setFailure(cause)
            throw cause
        }
        bindFuture.setSuccess(channel)
    }

    /**
     * 关闭底层 [ServerSocketChannel], 需注意这只会关掉 “listen”, 从而不再接受新的连接.
     * 原来 “accept” 的 [java.nio.channels.SocketChannel]  仍然正常工作.
     */
    private fun close0() {
        if (closed) {
            return
        }
        closed = true
        val name = javaChannel.toString()

        javaChannel.close()
        key.cancel()

        log.debug("Release resource associated with channel {}", name)
        closedFuture.setSuccess()
    }

    class SocketChannelEventHandler2 : SocketChannelEventHandler(null, null, null, null, null) {
        init {
            this.dispatcher.onConnected()
            this.connectableFuture.setSuccess(this.channel)
        }
    }
}
