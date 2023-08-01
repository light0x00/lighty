package io.github.light0x00.letty.core.handler

import io.github.light0x00.letty.core.BasicNioSocketChannel
import java.nio.channels.SocketChannel

/**
 * @author light0x00
 * @since 2023/7/9
 */
class AbstractNioSocketChannelImpl(private val javaChannel: SocketChannel) : NioSocketChannel, BasicNioSocketChannel(javaChannel) {

}