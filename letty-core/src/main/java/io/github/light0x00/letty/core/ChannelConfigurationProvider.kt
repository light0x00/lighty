package io.github.light0x00.letty.core

import io.github.light0x00.letty.core.handler.ChannelConfiguration
import io.github.light0x00.letty.core.handler.NioSocketChannel

/**
 * @author light0x00
 * @since 2023/7/9
 */
interface ChannelConfigurationProvider {

    fun configuration(channel: NioSocketChannel): ChannelConfiguration

}