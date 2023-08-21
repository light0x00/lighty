package io.github.light0x00.lighty.core.dispatcher

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.EventExecutor
import io.github.light0x00.lighty.core.facade.NioSocketChannel
import io.github.light0x00.lighty.core.handler.*
import io.github.light0x00.lighty.core.util.Loggable
import io.github.light0x00.lighty.core.util.Tool.getMethod
import io.github.light0x00.lighty.core.util.Tool.stackTraceToString
import io.github.light0x00.lighty.core.util.log
import lombok.extern.slf4j.Slf4j
import java.nio.channels.FileChannel
import java.util.*

/**
 * @author light0x00
 * @since 2023/8/2
 */
@Slf4j
class ChannelHandlerDispatcher(
    val context: ChannelContext,
    eventExecutor: EventExecutor,
    handlerExecutorPairs: List<ChannelHandlerExecutorPair<ChannelHandler>>,
    inboundReceiver: InboundPipelineInvocation,
    private val outboundReceiver: OutboundPipelineInvocation
) : Loggable {

    private var inboundChain: InboundPipelineInvocation
    private var outboundChain: OutboundPipelineInvocation
    private val initializeEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>
    private val destroyEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>
    private val connectedEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>
    private val readCompletedEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>
    private val closedEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>

    /**
     * Triggered when the connection established successfully
     */
    @get:JvmName("connectedFuture")
    val connectedFuture: ListenableFutureTask<Void> = ListenableFutureTask(eventExecutor)

    @get:JvmName("readCompletedFuture")
    val readCompletedFuture: ListenableFutureTask<Void> = ListenableFutureTask(eventExecutor)

    /**
     * Triggered when the connection closed.
     */
    @get:JvmName("closedFuture")
    val closedFuture: ListenableFutureTask<Void> = ListenableFutureTask<Void>(eventExecutor)

    init {
        val inboundHandlers = LinkedList<ChannelHandlerExecutorPair<InboundChannelHandler>>()
        val outboundHandlers = LinkedList<ChannelHandlerExecutorPair<OutboundChannelHandler>>()
        initializeEventObservers = HashSet()
        destroyEventObservers = HashSet()
        connectedEventObservers = HashSet()
        readCompletedEventObservers = HashSet()
        closedEventObservers = HashSet()

        for (pair in handlerExecutorPairs) {
            val (handler, _) = pair

            if (handler is InboundChannelHandler) {
                if (!skipReadEvent(handler)) {
                    @Suppress("UNCHECKED_CAST")
                    inboundHandlers.add(pair as ChannelHandlerExecutorPair<InboundChannelHandler>)
                }
            }

            if (handler is OutboundChannelHandler) {
                if (!skipWriteEvent(handler)) {
                    //出方向与入方向相反, 后加入的 handler 位于 pipeline 的前端
                    @Suppress("UNCHECKED_CAST")
                    outboundHandlers.addFirst(pair as ChannelHandlerExecutorPair<OutboundChannelHandler>)
                }
            }

            if (!skipInitializeEvent(handler)) {
                initializeEventObservers.add(pair)
            }

            if (!skipDestroyEvent(handler)) {
                destroyEventObservers.add(pair)
            }

            if (!skipConnectedEvent(handler)) {
                connectedEventObservers.add(pair)
            }

            if (!skipReadCompletedEvent(handler)) {
                readCompletedEventObservers.add(pair)
            }

            if (!skipClosedEvent(handler)) {
                closedEventObservers.add(pair)
            }
        }
        inboundChain = buildInboundInvocationChain(
            context, inboundHandlers, inboundReceiver
        )
        outboundChain = buildOutboundInvocationChain(
            context, outboundHandlers, outboundReceiver
        )
    }

    fun onConnected() {
        for ((handler, executor) in connectedEventObservers) {
            if (executor.inEventLoop()) {
                onConnected0(handler)
            } else {
                executor.execute { onConnected0(handler) }
            }
        }
        connectedFuture.setSuccess()
    }

    fun onInitialize() {
        for ((handler, executor) in initializeEventObservers) {
            if (executor.inEventLoop()) {
                onInitialize0(handler)
            } else {
                executor.execute { onInitialize0(handler) }
            }
        }
    }

    fun onDestroy() {
        for ((handler, executor) in destroyEventObservers) {
            if (executor.inEventLoop()) {
                onDestroy0(handler)
            } else {
                executor.execute { onDestroy0(handler) }
            }
        }
    }

    fun onReadCompleted() {
        for ((handler, executor) in readCompletedEventObservers) {
            if (executor.inEventLoop()) {
                onReadCompleted0(handler)
            } else {
                executor.execute { onReadCompleted0(handler) }
            }
        }
        readCompletedFuture.setSuccess()
    }

    fun onClosed() {
        for ((handler, executor) in closedEventObservers) {
            if (executor.inEventLoop()) {
                onClosed0(context, handler)
            } else {
                executor.execute { onClosed0(context, handler) }
            }
        }
        closedFuture.setSuccess()
    }

    fun input(buf: RecyclableBuffer): ListenableFutureTask<Void> {
        val future = ListenableFutureTask<Void>()
        inboundChain.invoke(buf, future)
        return future
    }

    fun output(data: Any, flush: Boolean): ListenableFutureTask<Void> {
        val writeFuture = ListenableFutureTask<Void>()
        outboundChain.invoke(data, writeFuture, flush)
        return writeFuture
    }

    fun transferTo(fc: FileChannel, flush: Boolean): ListenableFutureTask<Void> {
        val future = ListenableFutureTask<Void>()
        outboundReceiver.invoke(fc, future, flush)
        return future
    }

    private data class InboundPipelineInvocationImpl(
        val executor: EventExecutor,
        val handler: InboundChannelHandler,
        val context: ChannelContext,
        val next: InboundPipelineInvocation
    ) :
        InboundPipelineInvocation {
        override fun invoke(data: Any, upstreamFuture: ListenableFutureTask<Void>) {
            if (executor.inEventLoop()) {
                invoke0(data, upstreamFuture)
            } else {
                executor.execute { invoke0(data, upstreamFuture) }
            }
        }

        private fun invoke0(data: Any, upstreamFuture: ListenableFutureTask<Void>) {
            try {
                handler.onRead(context, data, object : InboundPipeline {

                    override fun next(data: Any?): ListenableFutureTask<Void> {
                        val future = ListenableFutureTask<Void>(null)
                        next.invoke(data, future)
                        return future
                    }

                    override fun upstreamFuture(): ListenableFutureTask<Void> {
                        return upstreamFuture
                    }
                })

            } catch (t: Throwable) {
                invokeExceptionCaught(handler, context, t)
            }
        }
    }

    private data class OutboundPipelineInvocationImpl(
        val executor: EventExecutor,
        val handler: OutboundChannelHandler,
        val context: ChannelContext,
        val next: OutboundPipelineInvocation
    ) : OutboundPipelineInvocation {
        override fun invoke(dataIn: Any, upstreamFuture: ListenableFutureTask<Void>, flush: Boolean) {
            if (executor.inEventLoop()) {
                invoke0(dataIn, upstreamFuture, flush)
            } else {
                executor.execute { invoke0(dataIn, upstreamFuture, flush) }
            }
        }

        private fun invoke0(upstreamData: Any, upstreamFuture: ListenableFutureTask<Void>, upstreamFlush: Boolean) {
            try {
                val downstreamContext = DownstreamChannelContext(context, next)
                handler.onWrite(downstreamContext, upstreamData,
                    object : OutboundPipeline {
                        override fun next(data: Any): ListenableFutureTask<Void> {
                            return if (upstreamFlush) {
                                downstreamContext.writeAndFlush(data)
                            } else {
                                downstreamContext.write(data)
                            }
                        }

                        override fun upstreamFuture(): ListenableFutureTask<Void> {
                            return upstreamFuture
                        }

                    })

            } catch (th: Throwable) {
                invokeExceptionCaught(handler, context, th)
            }
        }
    }

    private fun onConnected0(handler: ChannelHandler) {
        try {
            handler.onConnected(context)
        } catch (throwable: Throwable) {
            invokeExceptionCaught(handler, context, throwable)
        }
    }

    private fun onInitialize0(observer: ChannelHandler) {
        try {
            observer.onInitialize(context)
        } catch (throwable: Throwable) {
            invokeExceptionCaught(observer, context, throwable)
        }
    }

    private fun onDestroy0(observer: ChannelHandler) {
        try {
            observer.onDestroy(context)
        } catch (throwable: Throwable) {
            invokeExceptionCaught(observer, context, throwable)
        }
    }

    private fun onReadCompleted0(observer: ChannelHandler) {
        try {
            observer.onReadCompleted(context)
        } catch (throwable: Throwable) {
            invokeExceptionCaught(observer, context, throwable)
        }
    }

    private fun onClosed0(context: ChannelContext, observer: ChannelHandler) {
        try {
            observer.onClosed(context)
        } catch (throwable: Throwable) {
            invokeExceptionCaught(observer, context, throwable)
        }
    }


    companion object : Loggable {

        private fun invokeExceptionCaught(observer: ChannelHandler, context: ChannelContext, cause: Throwable) {
            if (skipExceptionCaught(observer)) {
                log.warn("An exception {} was thrown by handler {}", stackTraceToString(cause), observer)
                return
            }

            try {
                observer.exceptionCaught(context, cause)
            } catch (error: Throwable) {
                log.warn(
                    """
                            An exception {} was thrown by a user handler's exceptionCaught() method while handling the following exception:
                            """
                        .trimIndent(), stackTraceToString(error), cause
                )
            }
        }

        fun buildInboundInvocationChain(
            context: ChannelContext,
            pairs: List<ChannelHandlerExecutorPair<InboundChannelHandler>>,
            receiver: InboundPipelineInvocation
        ): InboundPipelineInvocation {

            var invocation = receiver
            for ((handler, executor) in pairs.asReversed()) {
                val next = invocation
                invocation = InboundPipelineInvocationImpl(executor, handler, context, next)
            }
            return invocation
        }

        fun buildOutboundInvocationChain(
            context: ChannelContext,
            pairs: List<ChannelHandlerExecutorPair<OutboundChannelHandler>>,
            receiver: OutboundPipelineInvocation
        ): OutboundPipelineInvocation {

            var invocation = receiver
            for ((handler, executor) in pairs.asReversed()) {
                val next = invocation
                invocation = OutboundPipelineInvocationImpl(executor, handler, context, next)
            }
            return invocation
        }

        private fun skipInitializeEvent(handler: ChannelHandler) =
            getMethod(handler, "onInitialize", ChannelContext::class.java)
                .isAnnotationPresent(Skip::class.java)

        private fun skipDestroyEvent(handler: ChannelHandler) =
            getMethod(handler, "onDestroy", ChannelContext::class.java)
                .isAnnotationPresent(Skip::class.java)

        private fun skipConnectedEvent(handler: ChannelHandler) =
            getMethod(handler, "onConnected", ChannelContext::class.java)
                .isAnnotationPresent(Skip::class.java)

        private fun skipClosedEvent(handler: ChannelHandler) =
            getMethod(handler, "onClosed", ChannelContext::class.java)
                .isAnnotationPresent(Skip::class.java)

        private fun skipReadCompletedEvent(handler: ChannelHandler) =
            getMethod(handler, "onReadCompleted", ChannelContext::class.java)
                .isAnnotationPresent(Skip::class.java)

        private fun skipExceptionCaught(handler: ChannelHandler) =
            getMethod(handler, "exceptionCaught", ChannelContext::class.java, Throwable::class.java)
                .isAnnotationPresent(Skip::class.java)

        private fun skipReadEvent(handler: ChannelHandler) =
            getMethod(
                handler,
                "onRead", ChannelContext::class.java, Any::class.java, InboundPipeline::class.java
            ).isAnnotationPresent(Skip::class.java)

        private fun skipWriteEvent(handler: ChannelHandler) =
            getMethod(
                handler,
                "onWrite", ChannelContext::class.java, Any::class.java, OutboundPipeline::class.java
            ).isAnnotationPresent(Skip::class.java)
    }

    /**
     * The flow of a normally write operation is:
     * ```
     *                                              │write operation
     *                                              ▼
     *  outboundBuffer◄────outboundHandler2◄────outboundHandler1
     * </pre>
     *
     * Thing seems goes well. Every write operation will go through the whole outbound pipeline.
     * Imagine what will happen if a write operation invoked by the `outboundHandler2`?
     *
     * ```
     *
     * ```
     *                                                │write operation
     *                                                ▼
     *  outboundBuffer◄────outboundHandler2◄────outboundHandler1
     *                             │                  ▲
     *                             └──────────────────┘
     *                                  write operation
     * ```
     * We will get an endless loop!
     * Obviously a write operation made by a [OutboundChannelHandler] cannot backtrack, it should just go forward.
     *
     * ```
     *                                                │write operation
     *                                                ▼
     *  outboundBuffer◄────outboundHandler2◄────outboundHandler1
     *         ▲                  │
     *         └──────────────────┘
     *            write operation
     * ```
     *
     * That's the reason why we need [DownstreamChannelContext]
     */
    class DownstreamChannelContext(
        private val context: ChannelContext,
        private val downstream: OutboundPipelineInvocation
    ) : ChannelContext by context {

        /**
         * 重写 write 方法, 使之将数据传给后续 handler, 而不是回到第一个, 造成死循环
         *
         * 调用 context.write 视为一次全新的写, 所以这里不沿用原 future; 这是与 OutboundPipeline#next 的区别
         */
        override fun write(data: Any): ListenableFutureTask<Void> {
            val future = ListenableFutureTask<Void>(null) //调用 context.write 视为一次全新的写, 所以这里不沿用原 future
            downstream.invoke(data, future, false)
            return future
        }

        override fun channel(): NioSocketChannel {
            return object : NioSocketChannel by this {}
        }

        override fun writeAndFlush(data: Any): ListenableFutureTask<Void> { //TODO 增加重载方法 允许外界传 promise
            val future = ListenableFutureTask<Void>(null)
            downstream.invoke(data, future, true)
            return future
        }

    }

}
