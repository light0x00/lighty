package io.github.light0x00.lighty.examples.time;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ServerBootstrap;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.ChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.OutboundPipeline;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Date;

/**
 * @author light0x00
 * @since 2023/8/24
 */
public class TimeServer {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        new ServerBootstrap()
                .group(group)
                .childInitializer(channel -> channel.pipeline()
                        .add(new TimeServerHandler(), new TimeEncoder()))
                .bind(new InetSocketAddress(9000));
    }

    private static class TimeServerHandler extends ChannelHandlerAdapter {
        @Override
        public void onConnected(@Nonnull ChannelContext context) {
            //1.
            context.writeAndFlush(new Date())
                    //2.
                    .addListener(future -> {
                        context.close();
                    });
        }
    }
    private static class TimeEncoder extends OutboundChannelHandlerAdapter {
        @Override
        public void onWrite(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull OutboundPipeline pipeline) {
            //1.
            var date = (Date) data;

            //2.
            RecyclableBuffer buf = context.allocateBuffer(8);
            buf.putLong(date.getTime());

            //3.
            pipeline.next(buf)
                    //4.
                    .addListener(future -> {
                        System.out.println("success:" + future.isSuccess());
                    })
                    //5.
                    .addListener(pipeline.upstreamFuture())
            ;
        }
    }
}
