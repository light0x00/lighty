package io.github.light0x00.lighty.examples.zerocopy;

import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.examples.common.IdentifierThreadFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author light0x00
 * @since 2023/8/5
 */
@Slf4j
public class ZeroCopyServer {

    public static void main(String[] args) {
        Path filePath = getFilepath(args);
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("server"));
        new ServerBootstrap()
                .group(eventLoopGroup)
                .childInitializer(channel -> channel.pipeline().add(new FileSender(filePath)))
                .bind(new InetSocketAddress(9000))
                .sync();
    }

    @Nonnull
    private static Path getFilepath(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Not yet specify filepath");
        }
        return Paths.get(args[0]);
    }

}
