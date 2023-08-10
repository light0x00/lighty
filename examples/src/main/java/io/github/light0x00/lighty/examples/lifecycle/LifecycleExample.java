package io.github.light0x00.lighty.examples.lifecycle;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.eventloop.SingleThreadExecutorGroup;
import io.github.light0x00.lighty.core.facade.ChannelInitializer;
import io.github.light0x00.lighty.core.facade.InitializingNioSocketChannel;
import io.github.light0x00.lighty.core.facade.NioServerSocketChannel;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.ChannelHandlerAdapter;
import io.github.light0x00.lighty.examples.IdentifierThreadFactory;
import io.github.light0x00.lighty.examples.utf32.UTF32Decoder;
import io.github.light0x00.lighty.examples.utf32.UTF32Encoder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;

/**
 * @author light0x00
 * @since 2023/8/10
 */
@Slf4j
public class LifecycleExample {

    public static void main(String[] args) {
//        log.debug("State transition: active->closed");
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("server"));
        SingleThreadExecutorGroup handlerExecutorGroup = new SingleThreadExecutorGroup(2, new IdentifierThreadFactory("user"));

        ListenableFutureTask<NioServerSocketChannel> future = new ServerBootstrap()
                .group(eventLoopGroup)
                .childInitializer(new ChannelInitializer<>() {
                    @Override
                    public void initChannel(InitializingNioSocketChannel channel) {
                        channel.pipeline().add(new UTF32Decoder());
                        channel.pipeline().add(new ChannelHandlerAdapter() {

                            @Override
                            public void onInitialize(ChannelContext context) {
                                super.onInitialize(context);
                            }

                            @Override
                            public void onDestroy(ChannelContext context) {
                                super.onDestroy(context);
                            }

                            @Override
                            public void onConnected(ChannelContext context) {
                                log.debug("State transition: active->closed");
                            }

                            @Override
                            public void onReadCompleted(ChannelContext context) {
                                super.onReadCompleted(context);
                            }

                            @Override
                            public void onClosed(ChannelContext context) {
                                super.onClosed(context);
                            }

                            @Override
                            public void exceptionCaught(ChannelContext context, Throwable t) {
                                super.exceptionCaught(context, t);
                            }
                        });
                        channel.pipeline().add(new UTF32Encoder());
                    }
                })
                .bind(new InetSocketAddress(9000));

        NioServerSocketChannel channel = future.get();

        log.info("Listen on {}", channel.localAddress());
    }

}
