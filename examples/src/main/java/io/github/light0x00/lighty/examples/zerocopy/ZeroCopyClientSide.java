package io.github.light0x00.lighty.examples.zerocopy;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.examples.common.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author light0x00
 * @since 2023/8/7
 */
@Slf4j
public class ZeroCopyClientSide {

    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("client"));

        ListenableFutureTask<NioSocketChannel> future = new ClientBootstrap()
                .group(eventLoopGroup)
                .initializer(channel -> {
                    channel.pipeline().add(new FileReceiver());
                })
                .connect(new InetSocketAddress(9000));

        if (future.isSuccess()) {
            NioSocketChannel channel = future.get();
            channel.closedFuture().sync();
        } else {
            future.cause().printStackTrace();
        }
        eventLoopGroup.shutdown();
    }

}
