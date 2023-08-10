package io.github.light0x00.lighty.examples.zerocopy;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.*;
import io.github.light0x00.lighty.examples.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;

/**
 * @author light0x00
 * @since 2023/8/7
 */
@Slf4j
public class ZeroCopyClientSide {

    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("client"));

        ListenableFutureTask<NioSocketChannel> connect = new ClientBootstrap()
                .group(eventLoopGroup)
                .properties(new DefaultLightyProperties() {

                    @Override
                    public int bufferPoolMaxSize() {
                        return 1024 * 1024 * 2;
                    }

                    @Override
                    public int readBufSize() {
                        return 131072; //19ms~26ms
//                        return 131072*2; //19ms
//                        return 131072*4; //17ms
//                        return 131072*8; //21~23ms
                    }

                })
                .initializer(new ChannelInitializer<>() {
                    @Override
                    public void initChannel(InitializingNioSocketChannel channel) {
                        log.info("socket receive buffer:{}", channel.getOption(StandardSocketOptions.SO_RCVBUF));
                        channel.pipeline().add(new FileReceiver());
                    }
                })
                .connect(new InetSocketAddress(9000));


        if (connect.isSuccess()) {
            NioSocketChannel channel = connect.get();
            channel.closeFuture().sync();
        } else {
            connect.cause().printStackTrace();
        }
        eventLoopGroup.shutdown();
    }

}
