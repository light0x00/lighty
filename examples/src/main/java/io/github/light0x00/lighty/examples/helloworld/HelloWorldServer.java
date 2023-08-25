package io.github.light0x00.lighty.examples.helloworld;

import io.github.light0x00.lighty.codec.StringDecoder;
import io.github.light0x00.lighty.codec.StringEncoder;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/8/25
 */
public class HelloWorldServer {
    public static void main(String[] args) {
        //1.
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //2.
        new ServerBootstrap()
                //3.
                .group(group)
                //4.
                .childInitializer(channel -> {
                    channel.pipeline().add(
                            new StringEncoder(StandardCharsets.UTF_8), //4.1
                            new StringDecoder(StandardCharsets.UTF_8), //4.2
                            new HelloWorldHandler());  //4.3
                })
                //5.
                .bind(new InetSocketAddress(9000));
    }

    private static class HelloWorldHandler extends InboundChannelHandlerAdapter { //1.
        @Override
        public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) { //2.
            //3.
            var msg = (String) data;
            System.out.println("Received:" + msg);
            //4.
            String reply = "Hello World";
            context.writeAndFlush(reply)
                    //5.
                    .addListener(future -> context.close());
        }
    }
}
