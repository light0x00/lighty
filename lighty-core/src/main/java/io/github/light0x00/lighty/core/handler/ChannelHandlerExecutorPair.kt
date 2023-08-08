package io.github.light0x00.lighty.core.handler

import io.github.light0x00.lighty.core.eventloop.EventExecutor
import io.github.light0x00.lighty.core.handler.adapter.ChannelHandler
import java.util.*

/**
 * @author light0x00
 * @since 2023/8/7
 */
data class ChannelHandlerExecutorPair<out T : ChannelHandler>(val handler: T, val executor: EventExecutor) {

    override fun hashCode(): Int {
        return Objects.hash(handler, executor)
    }

    override fun equals(obj: Any?): Boolean {
        return obj is ChannelHandlerExecutorPair<*> && handler == obj.handler && executor == obj.executor
    }
}