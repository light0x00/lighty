package io.github.light0x00.lighty.core

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup
import io.github.light0x00.lighty.core.facade.ClientBootstrap
import io.github.light0x00.lighty.core.facade.InitializingNioSocketChannel
import io.github.light0x00.lighty.core.facade.ServerBootstrap
import io.github.light0x00.lighty.core.handler.*
import org.junit.jupiter.api.AfterEach
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

    val group = NioEventLoopGroup(2)

    @AfterEach
    fun destroy() {
        group.shutdown().sync()
    }

    private fun address(): InetSocketAddress {
        return InetSocketAddress(9000)
    }

    /**
     * 测试 pipeline 中每个环节对上游的通知机制
     */
    @Test
    fun testPipelineUpstreamFutureNotify() {
        val notifyUpstreamTimes = AtomicInteger()

        val address = address()

        val serverChannel = ServerBootstrap()
            .group(group)
            .childInitializer { channel: InitializingNioSocketChannel ->
                channel.pipeline().add(
                    object : InboundChannelHandlerAdapter() {
                        override fun onRead(context: ChannelContext, data: Any, pipeline: InboundPipeline) {
                            pipeline.next(data)
                                .addListener { notifyUpstreamTimes.incrementAndGet() }
                                .addListener {
                                    context.close()
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
            .bind(address).sync()

        val channel = ClientBootstrap()
            .group(group)
            .connect(address).sync()

        channel.writeAndFlush("hello".toByteArray(StandardCharsets.UTF_8)).sync()
        channel.closedFuture().sync()
        serverChannel.close().sync()

        Assertions.assertEquals(2, notifyUpstreamTimes.get())
    }

    /**
     * 测试 outbound pipeline 中写操作造成无限循环问题
     */
    @Test
    fun testOutboundPipelineWriteEndlessLoop() {
        val address = address()

        val loopTimes = AtomicInteger()

        val serverChannel = ServerBootstrap()
            .group(group)
            .childInitializer() {
                it.pipeline()
                    .add(object : OutboundChannelHandlerAdapter() {
                        override fun onWrite(context: ChannelContext, data: Any, pipeline: OutboundPipeline) {
                            //invoke a write inside outbound pipeline
                            context.writeAndFlush(data).addListener(pipeline.upstreamFuture())
                        }
                    })
                    .add(object : OutboundChannelHandlerAdapter() {
                        override fun onConnected(context: ChannelContext) {
                            //write only for triggering outbound pipeline
                            context.writeAndFlush("1".toByteArray(StandardCharsets.UTF_8))
                                .addListener {
                                    context.close()
                                }
                        }

                        override fun onWrite(context: ChannelContext, data: Any, pipeline: OutboundPipeline) {
                            //denote that the data pass back the head of the outbound pipeline
                            //this will cause a endless loop
                            if (loopTimes.incrementAndGet() > 1) {
                                context.close()
                                return
                            }
                            pipeline.next(data).addListener(pipeline.upstreamFuture())
                        }
                    })
            }
            .bind(address)
            .sync()

        val channel = ClientBootstrap()
            .group(group)
            .connect(address)
            .sync()

        channel.closedFuture().sync()
        serverChannel.close()
        Assertions.assertEquals(1, loopTimes.get(), "Write inside outbound pipeline cause an endless loop")
    }


}