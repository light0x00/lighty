package io.github.light0x00.lighty.examples.echo;

import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ChannelInitializer;
import io.github.light0x00.lighty.core.facade.InitializingNioSocketChannel;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.examples.common.IdentifierThreadFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;

/**
 * @author light0x00
 * @since 2023/8/17
 */
public class EchoServer {
    public static void main(String[] args) {
        // 1. create an event-loop group, being used to handle to nio event and executing the handler's tasks.
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, new IdentifierThreadFactory("server"));

        // 2. Create a server builder
        new ServerBootstrap()
                //3. Specify the event-loop group
                .group(eventLoopGroup)
                //4. Specify an initializer for Configuring the accepted channel.
                .childInitializer(new ChannelInitializer<>() {
                    @Override
                    public void initChannel(@Nonnull InitializingNioSocketChannel channel) {
                        //4.1 Configure the pipeline for accepted channel
                        channel.pipeline().add(new EchoHandler());
                    }
                })
                //5. Bind an address
                .bind(new InetSocketAddress(9000))
                //6. Blocking util the binding action finished.
                .sync();
    }

    static class EchoHandler extends InboundChannelHandlerAdapter {
        @Override
        public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
            context.channel().write(data);  //1.
            context.channel().flush(); //2.
        }
    }
}
