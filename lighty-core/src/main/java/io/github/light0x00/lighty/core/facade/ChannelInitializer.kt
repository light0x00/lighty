package io.github.light0x00.lighty.core.facade

/**
 * @author light0x00
 * @since 2023/8/5
 */
interface ChannelInitializer<T : NioChannel> {
    fun initChannel(channel: T)

}
