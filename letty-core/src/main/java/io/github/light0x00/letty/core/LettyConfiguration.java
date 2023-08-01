package io.github.light0x00.letty.core;

import io.github.light0x00.letty.core.buffer.BufferPool;

/**
 * @author light0x00
 * @since 2023/7/31
 */
public interface LettyConfiguration {

    LettyProperties lettyProperties();

    BufferPool bufferPool();

    ChannelHandlerConfigurer handlerConfigurer();

}
