package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.EventExecutor
import io.github.light0x00.lighty.core.eventloop.EventExecutorGroup
import io.github.light0x00.lighty.core.handler.ChannelHandler
import io.github.light0x00.lighty.core.handler.ChannelHandlerExecutorPair
import java.nio.channels.FileChannel
import java.nio.channels.SocketChannel
import java.util.*

/**
 * The functionality of this channel decorator is limited.
 * Usually due to the underlying channel has not been fully constructed.
 * eg: The channel may be registered to a selector but not yet connected.
 * So that the read/write functionality is not available.
 *
 * @author light0x00
 * @since 2023/8/5
 */
class InitializingNioSocketChannel(
    javaChannel: SocketChannel, eventExecutor: EventExecutor
) :
    ChannelHandlerConfiguration,
    AbstractNioSocketChannel(javaChannel) {

    @get:JvmName("eventExecutor")
    var eventExecutor: EventExecutor = eventExecutor
        private set

    private val handlerExecutorPairs: MutableList<ChannelHandlerExecutorPair<ChannelHandler>> = LinkedList()

    private val pipeline = ChannelPipeline()

    /**
     * The [EventExecutorGroup] for executing handlers.
     */
    fun group(group: EventExecutorGroup<*>) : InitializingNioSocketChannel{
        eventExecutor = group.next()
        return this
    }

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

    override fun transfer(fc: FileChannel): ListenableFutureTask<Void> {
        throw LightyException("Unsupported operation!")
    }

    override fun transferAndFlush(fc: FileChannel): ListenableFutureTask<Void> {
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

        fun add(vararg handlers: ChannelHandler): ChannelPipeline {
            for (h in handlers) {
                handlerExecutorPairs.add(ChannelHandlerExecutorPair(h, eventExecutor))
            }
            return this
        }

        fun add(executorGroup: EventExecutorGroup<*>, vararg handlers: ChannelHandler): ChannelPipeline {
            val executor = executorGroup.next()
            for (h in handlers) {
                handlerExecutorPairs.add(ChannelHandlerExecutorPair(h, executor))
            }
            return this
        }
    }
}