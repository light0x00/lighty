package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.buffer.BufferPool;
import io.github.light0x00.letty.expr.eventloop.EventExecutorGroup;

import java.nio.ByteBuffer;
import java.util.List;

public interface ChannelHandlerConfigurer {

    default LettyConf lettyConf() {
        return new LettyConf() {
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

    default BufferPool<ByteBuffer> bufferPool() {
        return new BufferPool<>(ByteBuffer::allocateDirect);
    }

    EventExecutorGroup<?> executor();

    //
    List<ChannelInboundHandler> inboundPipelines();

    List<ChannelOutboundHandler> outboundPipelines();

    MessageHandler messageHandler();
}
