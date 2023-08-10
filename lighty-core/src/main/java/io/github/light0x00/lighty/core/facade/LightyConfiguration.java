package io.github.light0x00.lighty.core.facade;

import io.github.light0x00.lighty.core.buffer.BufferPool;

/**
 * @author light0x00
 * @since 2023/7/31
 */
public interface LightyConfiguration {

    LightyProperties lettyProperties();

    BufferPool bufferPool();

//    ChannelInitializer<InitializingNioSocketChannel> channelInitializer();

}
