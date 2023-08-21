package io.github.light0x00.lighty.core.handler

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.dispatcher.OutboundPipelineInvocation
import io.github.light0x00.lighty.core.facade.NioSocketChannel
import javax.annotation.concurrent.ThreadSafe


/**
 * @author light0x00
 * @since 2023/6/28
 */
@JvmDefaultWithoutCompatibility
@ThreadSafe
interface ChannelContext {

    fun allocateBuffer(capacity: Int): RecyclableBuffer

    fun channel(): NioSocketChannel

}