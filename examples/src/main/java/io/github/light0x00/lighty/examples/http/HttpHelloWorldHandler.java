package io.github.light0x00.lighty.examples.http;

import io.github.light0x00.lighty.codec.InboundMessageHandler;
import io.github.light0x00.lighty.codec.http.HttpRequest;
import io.github.light0x00.lighty.codec.http.HttpResponse;
import io.github.light0x00.lighty.codec.http.ResponseStatus;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/9/2
 */
public class HttpHelloWorldHandler extends InboundMessageHandler<HttpRequest> {

    @Override
    protected void handleInput(@Nonnull ChannelContext context, @Nonnull HttpRequest message, @Nonnull InboundPipeline pipeline) {
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
