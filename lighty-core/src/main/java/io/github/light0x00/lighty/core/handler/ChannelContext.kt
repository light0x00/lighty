package io.github.light0x00.lighty.core.handler

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer
import io.github.light0x00.lighty.core.facade.NioSocketChannel

/**
 * @author light0x00
 * @since 2023/8/21
 */
interface ChannelContext : NioSocketChannel {

    fun allocateBuffer(capacity: Int): RecyclableBuffer

    fun channel(): NioSocketChannel

}

