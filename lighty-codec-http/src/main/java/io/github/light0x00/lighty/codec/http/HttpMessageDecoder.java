package io.github.light0x00.lighty.codec.http;

import io.github.light0x00.lighty.codec.InboundMessageHandler;
import io.github.light0x00.lighty.core.buffer.ByteBuf;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/8/26
 */
@Slf4j
public class HttpMessageDecoder extends InboundMessageHandler<ByteBuf> {

    State state = State.Wait_Start_Line;

    HttpRequest request;

    int contentLength;

    ByteArrayOutputStream bodyBufferStream = new ByteArrayOutputStream(64);

    static final Charset charset = StandardCharsets.ISO_8859_1;

    @Override
    public void exceptionCaught(@Nonnull ChannelContext context, @Nonnull Throwable t) {
        log.error("", t);
        if (t instanceof MalformedHttpMessage) {
            byte[] payload = "Bad Request".getBytes(StandardCharsets.US_ASCII);
            var httpResponse = new HttpResponse();
            httpResponse.status(ResponseStatus.OK);
            httpResponse.version("HTTP/1.1");
            httpResponse.headers().put("content-type", "text/plain");
            httpResponse.headers().put("content-length", String.valueOf(payload.length));
            httpResponse.body(payload);
            context.writeAndFlush(httpResponse);
        }
    }

    @SneakyThrows
    @Override
    protected void handleInput(@Nonnull ChannelContext context, @Nonnull ByteBuf message, @Nonnull InboundPipeline pipeline) {
        try (message) {
            while (message.remainingCanGet() > 0) {
                if (state == State.Wait_Start_Line) {
                    var line = readLine(message);
                    if (line != null)
                        parseStartLine(line);
                } else if (state == State.Wait_Headers) {
                    var line = readLine(message);
                    if (line != null)
                        parseHeaders(pipeline, line);
                } else if (state == State.Wait_Body) {
                    parseBody(message, pipeline);
                } else {
                    throw new IllegalStateException("Unknown state " + state);
                }
            }
        }
    }

    private void parseStartLine(String line) {
        int i1 = line.indexOf(' ');
        int i2 = line.indexOf(' ', i1 + 1);
        if (i1 < 0 || i2 < 0) {
            throw new MalformedHttpMessage("Start line");
        }

        String p1 = line.substring(0, i1);
        String p2 = line.substring(i1 + 1, i2);
        String p3 = line.substring(i2);

        request = new HttpRequest();

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(p1);
        } catch (IllegalArgumentException e) {
            throw new MalformedHttpMessage("Unrecognized Method");
        }
        //暂不校验正确性
        request.method(method);
        request.requestPath(p2);
        request.version(p3);

        state = State.Wait_Headers;
    }

    private void parseHeaders(@Nonnull InboundPipeline pipeline, @Nonnull String line) {
        if (line.isEmpty()) {
            if (request.method().hasBody()) {
                //暂不考虑 chunked message
                String lenStr = request.headers().get("content-length");
                if (lenStr == null) {
                    throw new MalformedHttpMessage("Unknown Content-Length");
                }
                contentLength = Integer.parseInt(lenStr);
                if (contentLength < 0) {
                    throw new MalformedHttpMessage("Invalid Content-Length");
                }
                state = State.Wait_Body;
            } else {
                //请求解析完成
                state = State.Wait_Start_Line;
                pipeline.next(request);
            }
        } else {
            //解析 header
            int delimiterIdx = line.indexOf(":");
            if (delimiterIdx <= 0) {
                throw new MalformedHttpMessage("Invalid header");
            }
            String name = line.substring(0, delimiterIdx);
            String value = line.substring(delimiterIdx + 1);
            request.headers().put(name.toLowerCase(), value);
        }
    }

    private void parseBody(@Nonnull ByteBuf data, @Nonnull InboundPipeline pipeline) {
        if (contentLength < 0) {
            throw new MalformedHttpMessage("Unknown Content-Length");
        } else if (contentLength == 0) {
            return;
        }

        for (ByteBuffer slice : data.readableSlices()) {
            int bytesWritten = Math.min(contentLength - bodyBufferStream.size(), slice.remaining());
            bodyBufferStream.write(slice.array(), 0, bytesWritten);
            data.moveReadPosition(bytesWritten);

            if (bodyBufferStream.size() == contentLength) {
                request.body(bodyBufferStream.toByteArray()); //todo 考虑复用问题
                pipeline.next(request);
                state = State.Wait_Start_Line;
                bodyBufferStream.reset();
                contentLength = -1;
                break;
            }
        }
    }

    ByteArrayOutputStream lineBufferStream = new ByteArrayOutputStream(64);

    private String readLine(@Nonnull ByteBuf data) {
        boolean crlf = false;
        for (ByteBuffer buf : data.readableSlices()) {
            while (buf.remaining() > 0) {
                byte b = buf.get();
                data.moveReadPosition(1);
                if (b == '\r') {
                    crlf = true;
                } else if (crlf && b == '\n') {
                    String line = lineBufferStream.toString(charset);
                    lineBufferStream.reset();
                    return line;
                } else {
                    lineBufferStream.write(b);
                }
            }
        }
        return null;
    }

    enum State {
        Wait_Start_Line,
        Wait_Headers,
        Wait_Body
    }

}
