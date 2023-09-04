package io.github.light0x00.lighty.codec.http;

import io.github.light0x00.lighty.codec.OutboundMessageHandler;
import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/9/4
 */
public class HttpMessageEncoderTest {


    final static String CRLF = "\r\n";

    public static void main(String[] args) {
        ListenableFutureTask<String> future = new ListenableFutureTask<>();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        new ServerBootstrap()
                .group(group)
                .childInitializer(channel -> channel.pipeline()
                        .add(
                                new OutboundMessageHandler<byte[]>() {
                                    @Override
                                    protected void handleOutput(@Nonnull ChannelContext context, @Nonnull byte[] data, @Nonnull OutboundPipeline pipeline) {
                                        future.setSuccess(new String(data, StandardCharsets.US_ASCII));
                                    }
                                },
                                new HttpMessageEncoder(),
                                new InboundChannelHandlerAdapter() {
                                    @Override
                                    public void onConnected(@Nonnull ChannelContext context) {
                                        byte[] payload = "Hello World".getBytes(StandardCharsets.US_ASCII);

                                        var httpResponse = new HttpResponse();
                                        httpResponse.status(ResponseStatus.OK);
                                        httpResponse.version("HTTP/1.1");
                                        httpResponse.headers().put("content-type", "text/plain");
                                        httpResponse.headers().put("content-length", String.valueOf(payload.length));
                                        httpResponse.body(payload);

                                        context.writeAndFlush(httpResponse);
                                    }
                                }
                        ))
                .bind(new InetSocketAddress(9000)).sync();

        new ClientBootstrap()
                .group(group)
                .connect(new InetSocketAddress(9000)).sync();

        String r = future.get();
        group.shutdown();

        Assertions.assertEquals(
                "HTTP/1.1 200 OK" + CRLF +
                        "content-length: 11" + CRLF +
                        "content-type: text/plain" + CRLF +
                        CRLF +
                        "Hello World", r);
    }
}
