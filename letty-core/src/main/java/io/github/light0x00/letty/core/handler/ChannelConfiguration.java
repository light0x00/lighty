package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.LettyConfig;
import io.github.light0x00.letty.core.buffer.BufferPool;
import io.github.light0x00.letty.core.eventloop.EventLoopGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @Nullable
    default EventLoopGroup<?> handlerExecutor(){
        return null;
    }

    @Nonnull
    List<InboundChannelHandler> inboundHandlers();

    @Nonnull
    List<OutboundChannelHandler> outboundHandlers();

}
