package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.eventloop.EventExecutorGroup;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author light0x00
 * @since 2023/6/29
 */
@Slf4j
public class InTest {

    @Test
    public void test(){
        ListenableFutureTask<Void> fu = new ListenableFutureTask<Void>(null);

        fu.cancel(true);

        System.out.println(fu.isDone());

    }

    public static class ServerSide {
        public static void main(String[] args) {

            ExecutorService executorService = Executors.newFixedThreadPool(2, new IdentifierThreadFactory("server"));

            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, executorService);

            Server server = new Server(eventLoopGroup, new TestServerMessageHandler());

            server.bind(new InetSocketAddress("127.0.0.1", 9001))
                    .addListener((f) -> {

                    });
        }
    }

    public static class ClientSide {
        public static void main(String[] args) {
            ExecutorService executorService = Executors.newFixedThreadPool(2, new IdentifierThreadFactory("client"));
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, executorService);

            Client client = new Client(eventLoopGroup, new TestClientMessageHandler());

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

    static class TestClientMessageHandler implements ChannelHandlerConfigurer {

        @Override
        public EventExecutorGroup<?> executor() {
            return null;
        }

        @Override
        public List<ChannelInboundPipeline> inboundPipelines() {
            return null;
        }

        @Override
        public List<ChannelOutboundPipeline> outboundPipelines() {
            return null;
        }

        @Override
        public MessageHandler messageHandler() {
            return null;
        }


    }

    static class TestServerMessageHandler implements ChannelHandlerConfigurer {

        @Override
        public EventExecutorGroup<?> executor() {
            return null;
        }

        @Override
        public List<ChannelInboundPipeline> inboundPipelines() {
            return null;
        }

        @Override
        public List<ChannelOutboundPipeline> outboundPipelines() {
            return null;
        }

        @Override
        public MessageHandler messageHandler() {
            return null;
        }


    }
}
