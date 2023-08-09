package io.github.light0x00.lighty.core.facade

/**
 * @author light0x00
 * @since 2023/8/5
 */
interface ChannelInitializer {
    fun initChannel(channel: InitializingSocketChannel?)
}
