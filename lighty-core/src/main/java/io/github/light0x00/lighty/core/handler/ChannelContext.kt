package io.github.light0x00.lighty.core.handler

import io.github.light0x00.lighty.core.buffer.ByteBuf
import io.github.light0x00.lighty.core.facade.NioSocketChannel

/**
 * @author light0x00
 * @since 2023/8/21
 */
interface ChannelContext : NioSocketChannel {

    fun allocateBuffer(capacity: Int): ByteBuf

}

