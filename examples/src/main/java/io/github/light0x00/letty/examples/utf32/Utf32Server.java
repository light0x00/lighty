package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.ServerBootstrap;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.core.eventloop.SingleThreadExecutorGroup;
import io.github.light0x00.letty.core.facade.ChannelInitializer;
import io.github.light0x00.letty.core.facade.InitializingSocketChannel;
import io.github.light0x00.letty.core.handler.NioServerSocketChannel;
import io.github.light0x00.letty.examples.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author light0x00
 * @since 2023/7/27
 */
@Slf4j
public class Utf32Server {
    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("server"));
        SingleThreadExecutorGroup handlerExecutorGroup = new SingleThreadExecutorGroup(2, new IdentifierThreadFactory("user"));

        ListenableFutureTask<NioServerSocketChannel> future = new ServerBootstrap()
                .group(eventLoopGroup)
                .channelInitializer(new ChannelInitializer() {
                    @Override
                    public void initChannel(InitializingSocketChannel channel) {
                        channel.pipeline().add(new UTF32Decoder());
                        channel.pipeline().add(handlerExecutorGroup, new ServerMessageHandler());
                        channel.pipeline().add(handlerExecutorGroup, new UTF32Encoder());
                    }
                })
                .bind(new InetSocketAddress(9000));

        NioServerSocketChannel channel = future.get();

        log.info("Listen on {}", channel.getLocalAddress());
    }

}
