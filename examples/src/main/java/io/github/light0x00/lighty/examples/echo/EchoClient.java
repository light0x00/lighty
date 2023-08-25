package io.github.light0x00.lighty.examples.echo;

import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ChannelInitializer;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.InitializingNioSocketChannel;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.examples.common.IdentifierThreadFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;

/**
 * @author light0x00
 * @since 2023/8/22
 */
public class EchoClient {
    public static void main(String[] args) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, new IdentifierThreadFactory("server"));

        NioSocketChannel channel = new ClientBootstrap()
                .group(eventLoopGroup)
                .initializer(new ChannelInitializer<>() {
                    @Override
                    public void initChannel(@Nonnull InitializingNioSocketChannel channel) {
                        channel.pipeline().add(new InboundChannelHandlerAdapter() {
                            @Override
                            public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
                                System.out.println(data);
                            }
                        });
                    }
                })
                .connect(new InetSocketAddress(9000))
                .sync();

        channel.writeAndFlush("Hello World").sync();
        channel.close().sync();

        eventLoopGroup.shutdown();
    }
}
