package io.github.light0x00.lighty.examples.time;

import io.github.light0x00.lighty.codec.ByteToMessageDecoder;
import io.github.light0x00.lighty.core.buffer.RingBuffer;
import io.github.light0x00.lighty.core.eventloop.NioEventLoopGroup;
import io.github.light0x00.lighty.core.facade.ClientBootstrap;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Date;

/**
 * @author light0x00
 * @since 2023/8/25
 */
public class TimeClient {

    public static void main(String[] args) {
        //1.
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        NioSocketChannel channel = new ClientBootstrap() //2.
                //3.
                .group(group)
                //4.
                .initializer(ch -> {
                    ch.pipeline()
                            .add(new TimeDecoder(), new TimeClientHandler());

                })
                //5.
                .connect(new InetSocketAddress(9000))
                //6.
                .sync();

        //7.
        channel.closedFuture()
                .addListener(f -> group.shutdown());
    }

    private static class TimeDecoder extends ByteToMessageDecoder { //1.
        public TimeDecoder() {
            super(8); //1.
        }

        @Override
        protected void decode(ChannelContext context, RingBuffer buffer, InboundPipeline pipeline) {
            if (buffer.remainingCanGet() >= 8) { //2.
                long timestamp = buffer.getLong(); //3.
                Date date = new Date(timestamp); //4.
                pipeline.next(date); //5.
            }
        }
    }

    private static class TimeClientHandler extends InboundChannelHandlerAdapter {
        @Override
        public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
            var d = (Date) data;

            System.out.println("Time is " + d);
        }
    }
}
