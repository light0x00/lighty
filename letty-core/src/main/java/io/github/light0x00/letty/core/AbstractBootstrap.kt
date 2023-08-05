package io.github.light0x00.letty.core

import io.github.light0x00.letty.core.buffer.BufferPool
import io.github.light0x00.letty.core.buffer.DefaultByteBufferAllocator
import io.github.light0x00.letty.core.buffer.LruBufferPool

/**
 * @author light0x00
 * @since 2023/7/31
 */
abstract class AbstractBootstrap {

    companion object {
        val defaultProperties = DefaultLettyProperties()
        val defaultBufferPool = LruBufferPool(DefaultByteBufferAllocator(), defaultProperties.bufferPoolMaxSize())
    }

    protected fun buildConfiguration(
        lettyProperties: LettyProperties,
        bufferPool: BufferPool,
        channelInitializer: ChannelInitializer
    ): LettyConfiguration {
        return object : LettyConfiguration {
            override fun lettyProperties(): LettyProperties {
                return lettyProperties
            }

            override fun bufferPool(): BufferPool {
                return bufferPool;
            }

            override fun channelInitializer(): ChannelInitializer {
                return channelInitializer
            }
        }
    }
}