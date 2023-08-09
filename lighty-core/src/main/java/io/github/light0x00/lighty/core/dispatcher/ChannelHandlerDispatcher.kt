package io.github.light0x00.lighty.core.dispatcher

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.EventExecutor
import io.github.light0x00.lighty.core.handler.*
import io.github.light0x00.lighty.core.util.Loggable
import io.github.light0x00.lighty.core.util.Tool.getMethod
import io.github.light0x00.lighty.core.util.Tool.stackTraceToString
import io.github.light0x00.lighty.core.util.log
import lombok.extern.slf4j.Slf4j
import java.util.*

/**
 * @author light0x00
 * @since 2023/8/2
 */
@Slf4j
class ChannelHandlerDispatcher(
    private var eventExecutor: EventExecutor,
    val context: ChannelContext,
    handlerExecutorPairs: List<ChannelHandlerExecutorPair<ChannelHandler>>,
    inboundReceiver: InboundPipelineInvocation,
    outboundReceiver: OutboundPipelineInvocation
) : Loggable {

    private var inboundChain: InboundPipelineInvocation
    private var outboundChain: OutboundPipelineInvocation
    private val connectedEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>
    private val readCompletedEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>
    private val closedEventObservers: MutableSet<ChannelHandlerExecutorPair<ChannelHandler>>

    /**
     * Triggered when the connection established successfully
     */
    @get:JvmName("connectedFuture")
    val connectedFuture: ListenableFutureTask<Void> = ListenableFutureTask(null)

    /**
     * Triggered when the connection closed.
     */
    @get:JvmName("closedFuture")
    val closedFuture: ListenableFutureTask<Void> = ListenableFutureTask(null)

    init {
        val inboundHandlers = LinkedList<ChannelHandlerExecutorPair<InboundChannelHandler>>()
        val outboundHandlers = LinkedList<ChannelHandlerExecutorPair<OutboundChannelHandler>>()
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
                    @Suppress("UNCHECKED_CAST")
                    outboundHandlers.add(pair as ChannelHandlerExecutorPair<OutboundChannelHandler>)
                }
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
        inboundChain = buildInvocationChain(
            context, inboundHandlers, inboundReceiver
        )
        outboundChain = buildInvocationChain(
            context, outboundHandlers, outboundReceiver
        )
    }

    fun onConnected() {
        for ((handler, executor) in connectedEventObservers) {
            if (executor.inEventLoop()) {
                onConnected0(handler)
            } else {
                executor.execute {
                    onConnected0(handler)
                }
            }
        }
        connectedFuture.setSuccess()
    }

    fun onReadCompleted() {
        for ((handler, executor) in readCompletedEventObservers) {
            if (executor.inEventLoop()) {
                onReadCompleted0(handler)
            } else {
                executor.execute { onReadCompleted0(handler) }
            }
        }
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

    fun input(buf: RecyclableBuffer?) {
        if (eventExecutor.inEventLoop()) {
            inboundChain.invoke(buf)
        } else {
            eventExecutor.execute { inboundChain.invoke(buf) }
        }
    }

    fun output(data: Any): ListenableFutureTask<Void> {
        val writeFuture = ListenableFutureTask<Void>(null)
        if (eventExecutor.inEventLoop()) {
            outboundChain.invoke(data, writeFuture)
        } else {
            eventExecutor.execute { outboundChain.invoke(data, writeFuture) }
        }
        return writeFuture
    }

    private data class InboundPipelineInvocationImpl(
        val executor: EventExecutor,
        val handler: InboundChannelHandler,
        val context: ChannelContext,
        val next: InboundPipelineInvocation
    ) :
        InboundPipelineInvocation {
        override fun invoke(data: Any) {
            if (executor.inEventLoop()) {
                invoke0(data)
            } else {
                executor.execute { invoke0(data) }
            }
        }

        private fun invoke0(data: Any) {
            try {
                handler.onRead(context, data) { arg: Any -> next.invoke(arg) }
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
        override fun invoke(dataIn: Any, future: ListenableFutureTask<Void>) {
            if (executor.inEventLoop()) {
                invoke0(dataIn, future)
            } else {
                executor.execute { invoke0(dataIn, future) }
            }
        }

        private fun invoke0(dataIn: Any, future: ListenableFutureTask<Void>) {
            try {
                handler.onWrite(context.nextContext(next), dataIn)
                { dataOut: Any ->
                    next.invoke(dataOut, future)
                    future
                }
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

        fun buildInvocationChain(
            context: ChannelContext,
            handlers: List<ChannelHandlerExecutorPair<InboundChannelHandler>>,
            receiver: InboundPipelineInvocation
        ): InboundPipelineInvocation {

            var invocation = receiver
            for ((handler, executor) in handlers.asReversed()) {
                val next = invocation
                invocation = InboundPipelineInvocationImpl(executor, handler, context, next)
            }
            return invocation
        }

        fun buildInvocationChain(
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
}
