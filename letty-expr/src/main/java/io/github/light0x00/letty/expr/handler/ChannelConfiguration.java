package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.LettyConfig;
import io.github.light0x00.letty.expr.buffer.BufferPool;
import io.github.light0x00.letty.expr.eventloop.EventLoopGroup;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.List;

public interface ChannelConfiguration {

    default LettyConfig lettyConf() {
        return new LettyConfig() {
            @Override
            public boolean isAllowHalfClosure() {
                return false;
            }

            @Override
            public int readBufSize() {
                return 16;
            }
        };
    }

    default BufferPool bufferPool() {
        return new BufferPool(ByteBuffer::allocateDirect);
    }

    @Nonnull
    EventLoopGroup<?> handlerExecutor();

    @Nonnull
    List<InboundChannelHandler> inboundHandlers();

    @Nonnull
    List<OutboundChannelHandler> outboundHandlers();

}
