package io.github.light0x00.lighty.examples.docdrafts;

import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.core.handler.ChannelContext;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author light0x00
 * @since 2023/8/20
 */
public class Demo {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup(2);

        AtomicInteger notifyUpstreamTimes = new AtomicInteger();

        new ServerBootstrap()
                .group(group)
                .childInitializer(channel -> {
                    channel.pipeline().add(
                            new InboundChannelHandlerAdapter() {
                                @Override
                                public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
                                    pipeline.next(data).addListener(
                                            f -> {
                                                notifyUpstreamTimes.incrementAndGet();
                                            }
                                    );
                                }
                            },
                            new InboundChannelHandlerAdapter() {
                                @Override
                                public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
                                    pipeline.next(data)
                                            .addListener(
                                                    f -> {
                                                        notifyUpstreamTimes.incrementAndGet();
                                                    }
                                            ).addListener(
                                                    pipeline.upstreamFuture()
                                            )
                                    ;
                                }

                            }
                    );
                })
                .bind(new InetSocketAddress(9000));

        NioSocketChannel conn = new ClientBootstrap()
                .group(group)
                .initializer(channel -> {

                })
                .connect(new InetSocketAddress(9000)).sync();

        conn.writeAndFlush("hello".getBytes(StandardCharsets.UTF_8)).addListener(
                f -> {
                    f.get();
                }
        );
    }
}
