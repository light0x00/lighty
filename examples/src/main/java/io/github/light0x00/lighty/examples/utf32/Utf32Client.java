package io.github.light0x00.lighty.examples.utf32;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ChannelInitializer;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.InitializingSocketChannel;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.examples.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author light0x00
 * @since 2023/7/27
 */
@Slf4j
public class Utf32Client {
    public static void main(String[] args) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("io"));

        ListenableFutureTask<NioSocketChannel> connectFuture = new ClientBootstrap()
                .channelInitializer(new ChannelInitializer() {
                    @Override
                    public void initChannel(InitializingSocketChannel channel) {
                        channel.executorGroup(eventLoopGroup);

                        channel.pipeline().add(new UTF32Decoder());
                        channel.pipeline().add(new ClientMessageHandler());
                        channel.pipeline().add(new UTF32Encoder());

                    }
                })
                .group(eventLoopGroup)
                .connect(new InetSocketAddress("127.0.0.1", 9000));

        connectFuture.sync();
        NioSocketChannel channel = connectFuture.get();

        channel.closeFuture().sync();
        eventLoopGroup.shutdown();

//        new SocketChannelEventHandler(null,null,null,null,null){
//            {
//                this.connectableFuture.setSuccess();
//            }
//        };
    }

}
