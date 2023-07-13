package io.github.light0x00.letty.expr.examples.utf32;

import io.github.light0x00.letty.expr.*;
import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.examples.IdentifierThreadFactory;
import io.github.light0x00.letty.expr.handler.*;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
public class UTF32ServerClientTest {

    public static class ServerSide {
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

    public static class ClientSide {
        public static void main(String[] args) {
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("client"));

            Client client = new Client(eventLoopGroup,
                    channel -> new ChannelConfiguration() {

                        @Override
                        public List<InboundChannelHandler> inboundHandlers() {
                            return Arrays.asList(
                                    new UTF32Decoder(),
                                    new ClientMessageHandler()
                            );
                        }

                        @Override
                        public List<OutboundChannelHandler> outboundHandlers() {
                            return Arrays.asList(
                                    new UTF32Encoder()
                            );
                        }

                    }
            );

            ListenableFutureTask<NioSocketChannel> future = client.connect(new InetSocketAddress("127.0.0.1", 9001));
            NioSocketChannel channel = future.get();

            channel.closeFuture().addListener((f) -> {
                        log.info("Channel closed!");
                    }
            );
        }
    }


}
