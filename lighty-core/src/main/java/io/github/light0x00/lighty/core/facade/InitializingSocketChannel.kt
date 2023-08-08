package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.AbstractNioSocketChannel
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.EventExecutor
import io.github.light0x00.lighty.core.eventloop.EventExecutorGroup
import io.github.light0x00.lighty.core.handler.ChannelHandlerConfiguration
import io.github.light0x00.lighty.core.handler.ChannelHandlerExecutorPair
import io.github.light0x00.lighty.core.handler.adapter.ChannelHandler
import io.github.light0x00.lighty.core.util.LettyException
import java.nio.channels.SocketChannel
import java.util.*

/**
 * @author light0x00
 * @since 2023/8/5
 */
class InitializingSocketChannel(javaChannel: SocketChannel, var eventExecutor: EventExecutor) : ChannelHandlerConfiguration,
    AbstractNioSocketChannel(javaChannel) {

    private val handlers: MutableList<ChannelHandler> = LinkedList();

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

    override fun handlers(): MutableList<ChannelHandler> {
        return handlers
    }

    override fun handlerExecutorPair(): MutableList<ChannelHandlerExecutorPair<ChannelHandler>> {
        return handlerExecutorPairs;
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