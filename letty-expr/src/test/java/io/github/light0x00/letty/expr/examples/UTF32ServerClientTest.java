package io.github.light0x00.letty.expr.examples;

import io.github.light0x00.letty.expr.*;
import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.expr.handler.ChannelContext;
import io.github.light0x00.letty.expr.eventloop.EventLoop;
import io.github.light0x00.letty.expr.eventloop.EventLoopGroup;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.expr.handler.ChannelConfiguration;
import io.github.light0x00.letty.expr.handler.InboundChannelHandler;
import io.github.light0x00.letty.expr.handler.InboundPipeline;
import io.github.light0x00.letty.expr.handler.OutboundChannelHandler;
import io.github.light0x00.letty.expr.handler.adapter.ChannelHandlerAdapter;
import io.github.light0x00.letty.expr.handler.adapter.InboundChannelHandlerAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
public class UTF32ServerClientTest {

    public static class ServerSide {
        public static void main(String[] args) {

            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("server"));

            Server server = new Server(eventLoopGroup, channel -> new TestServerMessage(eventLoopGroup));

            server.bind(new InetSocketAddress("127.0.0.1", 9001))
                    .addListener((f) -> {
                        log.info("server started!");
                        NioServerSocketChannel ch = f.get();
                        log.info(ch.getLocalAddress().toString());
                    });

        }
    }

    public static class ClientSide {
        public static void main(String[] args) {
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("client"));

            Client client = new Client(eventLoopGroup,
                    channel -> new TestClientMessage(eventLoopGroup)
            );

            client.connect(new InetSocketAddress("127.0.0.1", 9001));
        }
    }


    static class IdentifierThreadFactory implements ThreadFactory {

        String identifier;
        AtomicInteger id = new AtomicInteger();

        public IdentifierThreadFactory(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            return new Thread(r, identifier + "-" + id.getAndIncrement());
        }
    }

    @AllArgsConstructor
    static class TestClientMessage implements ChannelConfiguration {

        EventLoopGroup<? extends EventLoop> executor;

        @Override
        public EventLoopGroup<?> handlerExecutor() {
            return executor;
        }

        @Override
        public List<InboundChannelHandler> inboundHandlers() {
            return
                    Arrays.asList(
                            new UTF32Decoder(),
                            new InboundChannelHandlerAdapter() {

                                @Override
                                public void onConnected(ChannelContext context) {
                                    context.write("Ohayo!");
                                }

                                @Override
                                public void onRead(ChannelContext context, Object data, InboundPipeline next) {
                                    log.info("onRead: {}", data);
                                }
                            }
                    );
        }

        @Override
        public List<OutboundChannelHandler> outboundHandlers() {
            return Arrays.asList(
                    new UTF32Encoder()
            );
        }
    }

    static class TestServerMessage implements ChannelConfiguration {
        EventLoopGroup<? extends EventLoop> executor;

        public TestServerMessage(EventLoopGroup<? extends EventLoop> executor) {
            this.executor = executor;
        }

        @Override
        public EventLoopGroup<?> handlerExecutor() {
            return executor;
        }

        @Override
        public List<InboundChannelHandler> inboundHandlers() {
            return Arrays.asList(
                    new UTF32Decoder(),
                    new ChannelHandlerAdapter() {

                        @Override
                        public void onConnected(ChannelContext context) {
                            context.write("hello world");
                            context.close();
                        }

                        @Override
                        public void onRead(ChannelContext context, Object data, InboundPipeline next) {
                            log.info("onRead: {}", data);
                        }
                    }
            );
        }

        @Override
        public List<OutboundChannelHandler> outboundHandlers() {
            return Arrays.asList(
                    new UTF32Encoder()
            );
        }

    }
}
