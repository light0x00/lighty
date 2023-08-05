package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.ChannelHandlerConfigurer;
import io.github.light0x00.letty.core.ChannelInitializer;
import io.github.light0x00.letty.core.InitializingSocketChannel;
import io.github.light0x00.letty.core.ServerBootstrap;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.core.handler.ChannelHandlerConfiguration;
import io.github.light0x00.letty.core.handler.NioServerSocketChannel;
import io.github.light0x00.letty.core.handler.NioSocketChannel;
import io.github.light0x00.letty.core.handler.adapter.InboundChannelHandler;
import io.github.light0x00.letty.core.handler.adapter.OutboundChannelHandler;
import io.github.light0x00.letty.examples.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * @author light0x00
 * @since 2023/7/27
 */
@Slf4j
public class ServerSide {
    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("server"));

        ListenableFutureTask<NioServerSocketChannel> future = new ServerBootstrap()
                .group(eventLoopGroup)
                .channelInitializer(new ChannelInitializer() {
                    @Override
                    public void initChannel(InitializingSocketChannel channel) {
                        channel.pipeline()
                                .add(
                                        new UTF32Decoder(),
                                        new ServerMessageHandler(),
                                        new UTF32Encoder()
                                );
                    }
                })
                .bind(new InetSocketAddress(9000));

        NioServerSocketChannel channel = future.get(); //blocking

        log.info("server started!");
    }

}
