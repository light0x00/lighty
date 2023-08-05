package io.github.light0x00.letty.core

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask
import io.github.light0x00.letty.core.eventloop.EventLoopGroup
import io.github.light0x00.letty.core.handler.ChannelHandlerConfiguration
import io.github.light0x00.letty.core.handler.adapter.ChannelObserver
import io.github.light0x00.letty.core.handler.adapter.DuplexChannelHandler
import io.github.light0x00.letty.core.handler.adapter.InboundChannelHandler
import io.github.light0x00.letty.core.handler.adapter.OutboundChannelHandler
import io.github.light0x00.letty.core.util.LettyException
import java.nio.channels.SocketChannel
import java.util.*

/**
 * @author light0x00
 * @since 2023/8/5
 */
class InitializingSocketChannel(javaChannel: SocketChannel) : ChannelHandlerConfiguration,
    AbstractNioSocketChannel(javaChannel) {

    private val inboundHandlers: MutableList<InboundChannelHandler> = LinkedList()
    private val outboundHandlers: MutableList<OutboundChannelHandler> = LinkedList()
    private val observers: MutableSet<ChannelObserver> = HashSet()

    @set:JvmName("executorGroup")
    var executorGroup: EventLoopGroup<*>? = null

    val pipeline = ChannelPipeline()

    fun pipeline(): ChannelPipeline {
        return pipeline
    }

    override fun executorGroup(): EventLoopGroup<*>? {
        return executorGroup
    }

    override fun observers(): Set<ChannelObserver> {
        return observers
    }

    override fun inboundHandlers(): List<InboundChannelHandler> {
        return inboundHandlers
    }

    override fun outboundHandlers(): List<OutboundChannelHandler> {
        return outboundHandlers
    }

    override fun write(data: Any): ListenableFutureTask<Void> {
        throw LettyException("operation not supported!");
    }

    override fun close(): ListenableFutureTask<Void> {
        throw LettyException("operation not supported!");
    }

    override fun connectedFuture(): ListenableFutureTask<Void> {
        throw LettyException("operation not supported!");
    }

    override fun closeFuture(): ListenableFutureTask<Void> {
        throw LettyException("operation not supported!");
    }

    override fun shutdownOutput(): ListenableFutureTask<Void> {
        throw LettyException("operation not supported!");
    }

    override fun shutdownInput(): ListenableFutureTask<Void> {
        throw LettyException("operation not supported!");
    }

    inner class ChannelPipeline {
        fun add(vararg handlers: DuplexChannelHandler) {
            for (h in handlers) {
                inboundHandlers.add(h)
                outboundHandlers.add(h)
                observers.add(h)
            }
        }

        fun add(vararg handlers: InboundChannelHandler) {
            for (h in handlers) {
                inboundHandlers.add(h)
                observers.add(h)
            }
        }

        fun add(vararg handlers: OutboundChannelHandler) {
            for (h in handlers) {
                outboundHandlers.add(h)
                observers.add(h)
            }
        }

        fun add(vararg handlers: ChannelObserver) {
            for (h in handlers) {
                if (h is InboundChannelHandler) {
                    inboundHandlers.add(h);
                }
                if (h is OutboundChannelHandler) {
                    outboundHandlers.add(h)
                }
                observers.add(h)
            }
        }

    }
}