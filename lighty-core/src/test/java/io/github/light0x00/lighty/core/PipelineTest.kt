package io.github.light0x00.lighty.core

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.lighty.core.facade.ClientBootstrap
import io.github.light0x00.lighty.core.facade.InitializingNioSocketChannel
import io.github.light0x00.lighty.core.facade.ServerBootstrap
import io.github.light0x00.lighty.core.handler.ChannelContext
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter
import io.github.light0x00.lighty.core.handler.InboundPipeline
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author light0x00
 * @since 2023/8/20
 */
class PipelineTest {

    /**
     * 测试 pipeline 中每个环节对上游的通知机制
     */
    @Test
    fun testPipelineUpstreamFutureNotify() {
        val group = NioEventLoopGroup(2)

        val notifyUpstreamTimes = AtomicInteger()

        ServerBootstrap()
            .group(group)
            .childInitializer { channel: InitializingNioSocketChannel ->
                channel.pipeline().add(
                    object : InboundChannelHandlerAdapter() {
                        override fun onRead(context: ChannelContext, data: Any, pipeline: InboundPipeline) {
                            pipeline.next(data)
                                .addListener { notifyUpstreamTimes.incrementAndGet() }
                                .addListener {
                                    context.channel().close()
                                }
                        }
                    },
                    object : InboundChannelHandlerAdapter() {
                        override fun onRead(context: ChannelContext, data: Any, pipeline: InboundPipeline) {
                            pipeline.next(data)
                                .addListener { f: ListenableFutureTask<Void> -> notifyUpstreamTimes.incrementAndGet() }
                                .addListener(
                                    pipeline.upstreamFuture()
                                )
                        }
                    }
                )
            }
            .bind(InetSocketAddress(9000))

        val conn = ClientBootstrap()
            .group(group)
            .connect(InetSocketAddress(9000)).sync()

        conn.writeAndFlush("hello".toByteArray(StandardCharsets.UTF_8)).sync()

        conn.closedFuture().sync()

        group.shutdown().sync()

        Assertions.assertEquals(2, notifyUpstreamTimes.get())
    }
}