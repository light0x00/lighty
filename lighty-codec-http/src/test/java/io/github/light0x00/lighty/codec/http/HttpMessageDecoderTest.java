package io.github.light0x00.lighty.codec.http;

import io.github.light0x00.lighty.codec.InboundMessageHandler;
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/9/4
 */
public class HttpMessageDecoderTest {

    public static void main(String[] args) {
        ListenableFutureTask<String> future = new ListenableFutureTask<>();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        new ServerBootstrap()
                .group(group)
                .childInitializer(channel -> channel.pipeline()
                        .add(
                                new HttpMessageDecoder(),
                                new InboundMessageHandler<HttpRequest>() {
                                    @Override
                                    protected void handleInput(@Nonnull ChannelContext context, @Nonnull HttpRequest message, @Nonnull InboundPipeline pipeline) {
                                        if (message.headers().getInt("content-length", 0) > 0)
                                            future.setSuccess(new String(message.body(), StandardCharsets.US_ASCII));
                                    }
                                }
                        ))
                .bind(new InetSocketAddress(9000)).sync();

        NioSocketChannel ch = new ClientBootstrap()
                .group(group)
                .connect(new InetSocketAddress(9000)).sync();

        ch.writeAndFlush("""
                POST / HTTP/1.1
                Content-Length: 0
                Content-Type: text/plain
                                
                """.getBytes(StandardCharsets.US_ASCII)).sync();

        ch.writeAndFlush("""
                POST / HTTP/1.1
                Content-Length: 11
                Content-Type: text/plain
                                
                Hello World""".getBytes(StandardCharsets.US_ASCII)).sync();

        Assertions.assertEquals("Hello World", future.get());

        group.shutdown();
    }
}
