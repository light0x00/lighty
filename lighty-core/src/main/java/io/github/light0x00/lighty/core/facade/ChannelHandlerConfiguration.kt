package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.eventloop.EventExecutorGroup
import io.github.light0x00.lighty.core.handler.ChannelHandler
import io.github.light0x00.lighty.core.handler.ChannelHandlerExecutorPair

interface ChannelHandlerConfiguration {
    fun handlerExecutorPair(): List<ChannelHandlerExecutorPair<ChannelHandler>>
}
