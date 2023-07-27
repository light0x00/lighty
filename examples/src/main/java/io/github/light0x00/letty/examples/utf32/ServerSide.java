package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.Server;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.core.handler.ChannelConfiguration;
import io.github.light0x00.letty.core.handler.InboundChannelHandler;
import io.github.light0x00.letty.core.handler.NioServerSocketChannel;
import io.github.light0x00.letty.core.handler.OutboundChannelHandler;
import io.github.light0x00.letty.examples.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

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

        Server server = new Server(eventLoopGroup, channel -> new ChannelConfiguration() {
            @Override
            public List<InboundChannelHandler> inboundHandlers() {
                return Arrays.asList(
                        new UTF32Decoder(),
                        new ServerMessageHandler()
                );
            }

            @Override
            public List<OutboundChannelHandler> outboundHandlers() {
                return Arrays.asList(
                        new UTF32Encoder()
                );
            }
        });

        ListenableFutureTask<NioServerSocketChannel> future = server.bind(new InetSocketAddress("127.0.0.1", 9001));
        NioServerSocketChannel channel = future.get(); //blocking

        log.info("server started!");
        log.info(channel.getLocalAddress().toString());
    }
}
