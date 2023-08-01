package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.ChannelHandlerConfigurer;
import io.github.light0x00.letty.core.ServerBootstrap;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.core.handler.*;
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
                .handlerConfigurer(new MyChannelHandlerConfigurer())
                .bind(new InetSocketAddress(9000));

        NioServerSocketChannel channel = future.get(); //blocking

        log.info("server started!");
    }

    private static class MyChannelHandlerConfigurer implements ChannelHandlerConfigurer {
        @Override
        public ChannelHandlerConfiguration configure(NioSocketChannel channel) {
            return new ChannelHandlerConfiguration() {

                @NotNull
                @Override
                public List<InboundChannelHandler> inboundHandlers() {
                    return Arrays.asList(
                            new UTF32Decoder(),
                            new ServerMessageHandler()
                    );
                }

                @NotNull
                @Override
                public List<OutboundChannelHandler> outboundHandlers() {
                    return Arrays.asList(
                            new UTF32Encoder()
                    );
                }

            };
        }
    }
}
