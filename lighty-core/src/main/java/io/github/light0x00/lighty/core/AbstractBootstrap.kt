package io.github.light0x00.lighty.core

import io.github.light0x00.lighty.core.buffer.BufferPool
import io.github.light0x00.lighty.core.buffer.DefaultByteBufferAllocator
import io.github.light0x00.lighty.core.buffer.LruBufferPool
import io.github.light0x00.lighty.core.facade.ChannelInitializer
import io.github.light0x00.lighty.core.util.LettyException

/**
 * @author light0x00
 * @since 2023/7/31
 */
@Suppress("UNCHECKED_CAST")
abstract class AbstractBootstrap<T : AbstractBootstrap<T>> {

    private var channelInitializer: ChannelInitializer? = null
    private var properties: LettyProperties? = null
    private var bufferPool: BufferPool? = null

    fun channelInitializer(channelInitializer: ChannelInitializer): T {
        this.channelInitializer = channelInitializer;
        return this as T
    }

    fun properties(properties: LettyProperties): T {
        this.properties = properties
        return this as T
    }

    fun bufferPool(pool: BufferPool): T {
        this.bufferPool = pool;
        return this as T
    }


    companion object {
        val defaultProperties = DefaultLettyProperties()
    }

    private fun validate(lettyProperties: LettyProperties) {
        if (lettyProperties.bufferPoolMaxSize() < lettyProperties.readBufSize()) {
            throw LettyException("Illegal properties, readBufSize should less that bufferPoolMaxSize")
        }
    }

    private fun newBufferPool(maxSize: Int): LruBufferPool {
        return LruBufferPool(DefaultByteBufferAllocator(), maxSize)
    }

    protected fun buildConfiguration(): LettyConfiguration {
        if (properties == null) {
            properties = defaultProperties
        } else {
            validate(properties!!)
        }
        if (bufferPool == null) {
            bufferPool = LruBufferPool(DefaultByteBufferAllocator(), properties!!.bufferPoolMaxSize())
        }
        if (channelInitializer == null) {
            throw LettyException("channelInitializer not set")
        }
        return newConfiguration(properties!!, bufferPool!!, channelInitializer!!)
    }

    private fun newConfiguration(
        lettyProperties: LettyProperties,
        bufferPool: BufferPool,
        channelInitializer: ChannelInitializer
    ): LettyConfiguration {
        return object : LettyConfiguration {
            override fun lettyProperties(): LettyProperties {
                return lettyProperties
            }

            override fun bufferPool(): BufferPool {
                return bufferPool
            }

            override fun channelInitializer(): ChannelInitializer {
                return channelInitializer
            }
        }
    }
}