package io.github.light0x00.letty.examples.utf32;

import io.github.light0x00.letty.core.ChannelHandlerConfigurer;
import io.github.light0x00.letty.core.ClientBootstrap;
import io.github.light0x00.letty.core.concurrent.FutureListener;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventLoopGroup;
import io.github.light0x00.letty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.core.eventloop.SingleThreadExecutorGroup;
import io.github.light0x00.letty.core.handler.ChannelHandlerConfiguration;
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


        ListenableFutureTask<NioSocketChannel> channelFuture = new ClientBootstrap()
                .handlerConfigurer(new ChannelHandlerConfigurer() {
                    @Override
                    public ChannelHandlerConfiguration configure(NioSocketChannel channel) {
                        return new MyChannelHandlerConfiguration();
                    }
                })
                .group(eventLoopGroup)
                .connect(new InetSocketAddress("127.0.0.1", 9000));
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

    private static class MyChannelHandlerConfiguration implements ChannelHandlerConfiguration {

        SingleThreadExecutorGroup singleThreadExecutorGroup = new SingleThreadExecutorGroup(2, new IdentifierThreadFactory("handler"));

        List<InboundChannelHandler> inboundHandlers = Arrays.asList(
                new UTF32Decoder(),
                new ClientMessageHandler()
        );
        List<OutboundChannelHandler> outboundHandlers = Arrays.asList(
                new UTF32Encoder()
        );

        @Nullable
        @Override
        public EventLoopGroup<?> handlerExecutor() {
            return singleThreadExecutorGroup;
        }

        @Override
        public List<InboundChannelHandler> inboundHandlers() {
            return inboundHandlers;
        }

        @Override
        public List<OutboundChannelHandler> outboundHandlers() {
            return outboundHandlers;
        }

    }
}
