package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.eventloop.EventExecutor;
import io.github.light0x00.letty.expr.eventloop.EventExecutorGroup;
import io.github.light0x00.letty.expr.eventloop.NioEventLoopGroup;
import io.github.light0x00.letty.expr.handler.ChannelHandlerConfigurer;
import io.github.light0x00.letty.expr.handler.InboundChannelHandler;
import io.github.light0x00.letty.expr.handler.InboundPipeline;
import io.github.light0x00.letty.expr.handler.OutboundChannelHandler;
import io.github.light0x00.letty.expr.handler.adapter.InboundChannelHandlerAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
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
    public void test() {
        ListenableFutureTask<Void> fu = new ListenableFutureTask<Void>(null);

        fu.cancel(true);

        System.out.println(fu.isDone());

    }

    public static class ServerSide {
        public static void main(String[] args) {

            ExecutorService executorService = Executors.newFixedThreadPool(2, new IdentifierThreadFactory("server"));

            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, executorService);

            Server server = new Server(eventLoopGroup, new TestServerMessageHandler(eventLoopGroup));

            server.bind(new InetSocketAddress("127.0.0.1", 9001))
                    .addListener((f) -> {
                        log.info("server started!");
                    });
        }
    }

    public static class ClientSide {
        public static void main(String[] args) {
            ExecutorService executorService = Executors.newFixedThreadPool(2, new IdentifierThreadFactory("client"));
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, executorService);

            Client client = new Client(eventLoopGroup, new TestClientMessageHandler(eventLoopGroup));

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
    static class TestClientMessageHandler implements ChannelHandlerConfigurer {

        EventExecutorGroup<? extends EventExecutor> executor;

        @Override
        public EventExecutorGroup<?> executorGroup() {
            return executor;
        }

        @Override
        public List<InboundChannelHandler> inboundHandlers() {
            return
                    Arrays.asList(
                            new UTF32Decoder(),
                            new InboundChannelHandlerAdapter(){
                                @Override
                                public void onRead(ChannelContext context, Object data, InboundPipeline next) {
                                    log.info("onRead: {}",data);
                                }
                            }
                    );
        }

        @Override
        public List<OutboundChannelHandler> outboundHandlers() {
            return Collections.emptyList();
        }

    }

    static class TestServerMessageHandler implements ChannelHandlerConfigurer {
        EventExecutorGroup<? extends EventExecutor> executor;

        public TestServerMessageHandler(EventExecutorGroup<? extends EventExecutor> executor) {
            this.executor = executor;
        }

        @Override
        public EventExecutorGroup<?> executorGroup() {
            return executor;
        }

        @Override
        public List<InboundChannelHandler> inboundHandlers() {
            return Arrays.asList(
                    new InboundChannelHandlerAdapter() {
                        @Override
                        public void onConnected(ChannelContext context) {
                            context.write("hello world");
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
