package io.github.light0x00.lighty.examples.txtreader;

import io.github.light0x00.lighty.codec.StringDecoder;
import io.github.light0x00.lighty.codec.StringEncoder;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.NioServerSocketChannel;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.examples.common.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/7/27
 */
@Slf4j
@SuppressWarnings("Duplicates")
public class TxtReaderServer {

    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("server"));

        NioServerSocketChannel channel = new ServerBootstrap()
                .group(eventLoopGroup)
                .childInitializer(ch -> {
                    ch.pipeline().add(
                            new StringDecoder(StandardCharsets.UTF_8),
                            new StringEncoder(StandardCharsets.UTF_8),
                            new LineBaseMessageDecoder(),
                            new TxtReaderServerHandler()
                    );
                })
                .bind(new InetSocketAddress(9000)).sync();

        log.info("Listen on {}", channel.localAddress());
    }

}
