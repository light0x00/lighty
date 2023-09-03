package io.github.light0x00.lighty.examples.http;

import io.github.light0x00.lighty.codec.http.HttpMessageDecoder;
import io.github.light0x00.lighty.codec.http.HttpMessageEncoder;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;

import java.net.InetSocketAddress;

/**
 * @author light0x00
 * @since 2023/8/26
 */
public class HttpHelloWorldServer {

    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        new ServerBootstrap()
                .group(group)
                .childInitializer(channel -> {
                    channel.pipeline()
                            .add(
                                    new HttpMessageDecoder(),
                                    new HttpMessageEncoder(),
                                    new HelloWorldHandler()
                            );
                })
                .bind(new InetSocketAddress(9000));
    }
}
