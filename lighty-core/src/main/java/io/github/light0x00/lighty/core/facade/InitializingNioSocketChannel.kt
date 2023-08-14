package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.EventExecutor
import io.github.light0x00.lighty.core.eventloop.EventExecutorGroup
import io.github.light0x00.lighty.core.handler.ChannelHandler
import io.github.light0x00.lighty.core.handler.ChannelHandlerExecutorPair
import java.nio.channels.SocketChannel
import java.util.*

/**
 * The functionality of this channel decorator is limited.
 * Usually due to the underlying channel has not been fully constructed.
 * eg: The tha channel may be registered to a selector but not yet connected.
 * So that the read/write functionality is not available.
 *
 * @author light0x00
 * @since 2023/8/5
 */
class InitializingNioSocketChannel(javaChannel: SocketChannel, private var eventExecutor: EventExecutor) :
    ChannelHandlerConfiguration,
    AbstractNioSocketChannel(javaChannel) {

    private val handlerExecutorPairs: MutableList<ChannelHandlerExecutorPair<ChannelHandler>> = LinkedList();

    /**
     * The [EventExecutorGroup] for executing handlers.
     */
    @set:JvmName("executorGroup")
    var executorGroup: EventExecutorGroup<*>? = null
        set(value) {
            if (value == null)
                throw IllegalArgumentException("null")
            field = value
            eventExecutor = value.next()
        }

    private val pipeline = ChannelPipeline()

    fun pipeline(): ChannelPipeline {
        return pipeline
    }

    override fun handlerExecutorPair(): MutableList<ChannelHandlerExecutorPair<ChannelHandler>> {
        return handlerExecutorPairs
    }

    override fun write(data: Any): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun flush() {
        throw LightyException("Unsupported operation!")
    }

    override fun writeAndFlush(data: Any): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun close(): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun connectedFuture(): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun closedFuture(): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun shutdownOutput(): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun shutdownInput(): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    inner class ChannelPipeline {

        fun add(vararg handlers: ChannelHandler) {
            for (h in handlers) {
                handlerExecutorPairs.add(ChannelHandlerExecutorPair(h, eventExecutor))
            }
        }

        fun add(executorGroup: EventExecutorGroup<*>, vararg handlers: ChannelHandler) {
            val executor = executorGroup.next()
            for (h in handlers) {
                handlerExecutorPairs.add(ChannelHandlerExecutorPair(h, executor))
            }
        }
    }
}