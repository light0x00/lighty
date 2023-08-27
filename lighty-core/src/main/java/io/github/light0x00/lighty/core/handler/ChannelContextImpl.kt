package io.github.light0x00.lighty.core.handler

import io.github.light0x00.lighty.core.buffer.BufferPool
import io.github.light0x00.lighty.core.buffer.ByteBuf
import io.github.light0x00.lighty.core.facade.NioSocketChannel

class ChannelContextImpl(
    private val channel: NioSocketChannel,
    private val bufferPool: BufferPool
) :
    ChannelContext,
    NioSocketChannel by channel {

    override fun allocateBuffer(capacity: Int): ByteBuf {
        return bufferPool.take(capacity)
    }
}