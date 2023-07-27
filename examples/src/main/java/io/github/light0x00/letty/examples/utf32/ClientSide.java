package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.Client;
import io.github.light0x00.letty.core.concurrent.FutureListener;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventLoopGroup;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.core.eventloop.SingleThreadExecutorGroup;
import io.github.light0x00.letty.core.handler.ChannelConfiguration;
import io.github.light0x00.letty.core.handler.InboundChannelHandler;
import io.github.light0x00.letty.core.handler.NioSocketChannel;
import io.github.light0x00.letty.core.handler.OutboundChannelHandler;
import io.github.light0x00.letty.examples.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * @author light0x00
 * @since 2023/7/27
 */
@Slf4j
public class ClientSide {
    public static void main(String[] args) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("io"));

        SingleThreadExecutorGroup singleThreadExecutorGroup = new SingleThreadExecutorGroup(2, new IdentifierThreadFactory("handler"));

        Client client = new Client(eventLoopGroup,
                channel -> new ChannelConfiguration() {

                    @Nullable
                    @Override
                    public EventLoopGroup<?> handlerExecutor() {
                        return singleThreadExecutorGroup;
                    }

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

        ListenableFutureTask<NioSocketChannel> channelFuture = client.connect(new InetSocketAddress("127.0.0.1", 9001));

        channelFuture.addListener(new FutureListener<NioSocketChannel>() {
            @Override
            public void operationComplete(ListenableFutureTask<NioSocketChannel> channelFuture) {
                if (channelFuture.isSuccess()) {
                    log.info("connected!");

                    NioSocketChannel channel = channelFuture.get();

                    channel.closeFuture().addListener((f) -> log.info("Channel closed!"));
                } else {
                    log.error("connected failed!");
                }
            }
        });


    }
}
