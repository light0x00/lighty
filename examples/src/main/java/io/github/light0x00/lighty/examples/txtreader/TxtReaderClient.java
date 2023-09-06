package io.github.light0x00.lighty.examples.txtreader;

import io.github.light0x00.lighty.codec.StringDecoder;
import io.github.light0x00.lighty.codec.StringEncoder;
import io.github.light0x00.lighty.core.concurrent.FailureFutureListener;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.eventloop.DefaultEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.DuplexChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.examples.common.IdentifierThreadFactory;
import io.github.light0x00.lighty.examples.common.InputMsgFromCommandLine;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static ch.qos.logback.classic.Level.INFO;

/**
 * @author light0x00
 * @since 2023/7/27
 */
@Slf4j
public class TxtReaderClient {

    public static void main(String[] args) {
        offDebugLog();

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2, new IdentifierThreadFactory("event-loop"));
        DefaultEventLoopGroup pipelineGroup = new DefaultEventLoopGroup(1, new IdentifierThreadFactory("pipeline"));

        NioSocketChannel channel = new ClientBootstrap()
                .initializer(ch ->
                        ch
                                .group(pipelineGroup)
                                .pipeline().add(
                                        new StringEncoder(StandardCharsets.UTF_8),
                                        new StringDecoder(StandardCharsets.UTF_8),
                                        new LineBaseMessageEncoder(),
                                        new DuplexChannelHandlerAdapter() {
                                            @Override
                                            public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
                                                System.out.print(data);
                                            }
                                        }
                                ))
                .group(eventLoopGroup)
                .connect(new InetSocketAddress(9000)).sync();

        channel.closedFuture()
                .addListener(new FailureFutureListener<>(Throwable::printStackTrace))
                .addListener((f) -> eventLoopGroup.shutdown())
                .addListener((f) -> pipelineGroup.shutdown());

        new InputMsgFromCommandLine(channel).run();
    }

    private static void offDebugLog() {
        Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ((ch.qos.logback.classic.Logger) root).setLevel(INFO);
    }
}
