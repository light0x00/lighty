package io.github.light0x00.letty.expr

import io.github.light0x00.letty.expr.handler.ChannelConfiguration

/**
 * @author light0x00
 * @since 2023/7/9
 */
interface ChannelConfigurationProvider {

    fun configuration(channel: NioSocketChannel): ChannelConfiguration

}