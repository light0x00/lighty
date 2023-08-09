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
class InitializingNioSocketChannel(javaChannel: SocketChannel, var eventExecutor: EventExecutor) :
    ChannelHandlerConfiguration,
    AbstractNioSocketChannel(javaChannel) {

    private val handlerExecutorPairs: MutableList<ChannelHandlerExecutorPair<ChannelHandler>> = LinkedList();

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

    override fun executorGroup(): EventExecutorGroup<*>? {
        return executorGroup
    }

    override fun handlerExecutorPair(): MutableList<ChannelHandlerExecutorPair<ChannelHandler>> {
        return handlerExecutorPairs;
    }

    override fun write(data: Any): ListenableFutureTask<Void> {
        throw LightyException("operation not supported!");
    }

    override fun close(): ListenableFutureTask<Void> {
        throw LightyException("operation not supported!");
    }

    override fun connectedFuture(): ListenableFutureTask<Void> {
        throw LightyException("operation not supported!");
    }

    override fun closeFuture(): ListenableFutureTask<Void> {
        throw LightyException("operation not supported!");
    }

    override fun shutdownOutput(): ListenableFutureTask<Void> {
        throw LightyException("operation not supported!");
    }

    override fun shutdownInput(): ListenableFutureTask<Void> {
        throw LightyException("operation not supported!");
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