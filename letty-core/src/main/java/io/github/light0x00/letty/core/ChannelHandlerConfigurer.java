package io.github.light0x00.letty.core;

import io.github.light0x00.letty.core.handler.ChannelHandlerConfiguration;
import io.github.light0x00.letty.core.handler.NioSocketChannel;

/**
 * @author light0x00
 * @since 2023/7/31
 */
public interface ChannelHandlerConfigurer {

    ChannelHandlerConfiguration configure(NioSocketChannel channel);

}
