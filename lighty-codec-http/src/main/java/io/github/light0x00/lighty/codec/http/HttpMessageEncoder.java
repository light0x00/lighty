package io.github.light0x00.lighty.codec.http;

import io.github.light0x00.lighty.codec.OutboundMessageHandler;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author light0x00
 * @since 2023/8/26
 */
@Slf4j
public class HttpMessageEncoder extends OutboundMessageHandler<HttpResponse> {

    ByteArrayOutputStream encodeBufferStream = new ByteArrayOutputStream();

    /**
     * Historically, HTTP has allowed field content with text in the
     * ISO-8859-1 charset [ISO-8859-1], supporting other charsets only
     * through use of [RFC2047] encoding.  In practice, most HTTP header
     * field values use only a subset of the US-ASCII charset [USASCII].
     * Newly defined header fields SHOULD limit their field values to
     * US-ASCII octets.  A recipient SHOULD treat other octets in field
     * content (obs-text) as opaque data.
     * <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.4">reference</a>
     */
    static final Charset charset = StandardCharsets.ISO_8859_1;

    @SneakyThrows
    @Override
    protected void handleOutput(@Nonnull ChannelContext context, @Nonnull HttpResponse data, @Nonnull OutboundPipeline pipeline) {
        encodeBufferStream.write(data.version().getBytes(charset));
        encodeBufferStream.write(HttpConstants.SP);
        encodeBufferStream.write(String.valueOf(data.status().code()).getBytes(charset));
        encodeBufferStream.write(HttpConstants.SP);
        encodeBufferStream.write(data.status().text().getBytes(charset));

        encodeBufferStream.write(HttpConstants.CRLF);

        for (Map.Entry<String, String> entry : data.headers().entrySet()) {
            encodeBufferStream.write(entry.getKey().getBytes(charset));
            encodeBufferStream.write(HttpConstants.COLON_SP);
            if (entry.getValue() != null)
                encodeBufferStream.write(entry.getValue().getBytes(charset));
            encodeBufferStream.write(HttpConstants.CRLF);
        }
        encodeBufferStream.write(HttpConstants.CRLF);
        if (data.body() != null) {
            encodeBufferStream.write(data.body());
        }
        pipeline.next(encodeBufferStream.toByteArray());
    }

    @Override
    public void exceptionCaught(@Nonnull ChannelContext context, @Nonnull Throwable t) {
        log.error("",t);
    }
}
