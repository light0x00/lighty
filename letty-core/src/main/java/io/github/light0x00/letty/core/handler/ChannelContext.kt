package io.github.light0x00.letty.core.handler

import io.github.light0x00.letty.core.buffer.RecyclableByteBuffer
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask


/**
 * @author light0x00
 * @since 2023/6/28
 */
@JvmDefaultWithoutCompatibility
interface ChannelContext {

    fun allocateBuffer(capacity: Int): RecyclableByteBuffer

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
        val ref = this;
        //
        return object : ChannelContext by this {
            override fun channel(): NioSocketChannel {
                //
                return object : NioSocketChannel by this@ChannelContext.channel() {
                    override fun write(data: Any): ListenableFutureTask<Void> {
                        val future = ListenableFutureTask<Void>(null)
                        outboundPipeline.invoke(data, future);
                        return future
                    }
                }
            }

        }
    }
}