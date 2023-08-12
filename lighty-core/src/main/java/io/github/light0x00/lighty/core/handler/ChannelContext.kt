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
     * That's the reason why we need [nextContext]
     */
    fun nextContext(outboundPipeline: OutboundPipelineInvocation): ChannelContext {
        //重写 context
        return object : ChannelContext by this {
            override fun channel(): NioSocketChannel {
                //重写 channel
                return object : NioSocketChannel by this@ChannelContext.channel() {
                    /**
                     * 重写 write 方法, 使之将数据传给后续 handler, 而不是回到第一个, 造成死循环
                     *
                     * 调用 context.write 视为一次全新的写, 所以这里不沿用原 future; 这是与 OutboundPipeline#next 的区别
                     */
                    override fun write(data: Any): ListenableFutureTask<Void> {
                        val future = ListenableFutureTask<Void>(null) //调用 context.write 视为一次全新的写, 所以这里不沿用原 future
                        outboundPipeline.invoke(data, future, false);
                        return future
                    }

                    override fun writeAndFlush(data: Any): ListenableFutureTask<Void> {
                        val future = ListenableFutureTask<Void>(null)
                        outboundPipeline.invoke(data, future, true);
                        return future
                    }
                }
            }

        }
    }
}