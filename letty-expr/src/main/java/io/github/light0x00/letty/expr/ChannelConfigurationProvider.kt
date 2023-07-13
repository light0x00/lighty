package io.github.light0x00.letty.expr

import io.github.light0x00.letty.expr.handler.ChannelConfiguration
import io.github.light0x00.letty.expr.handler.NioSocketChannel
import io.github.light0x00.letty.expr.handler.NioSocketChannelImpl

/**
 * @author light0x00
 * @since 2023/7/9
 */
interface ChannelConfigurationProvider {

    fun configuration(channel: NioSocketChannel): ChannelConfiguration

}