package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.buffer.BufferPool
import io.github.light0x00.lighty.core.buffer.DefaultByteBufferAllocator
import io.github.light0x00.lighty.core.buffer.LruBufferPool
import java.util.function.Supplier

/**
 * @author light0x00
 * @since 2023/7/31
 */
@Suppress("UNCHECKED_CAST")
abstract class AbstractBootstrap<T : AbstractBootstrap<T>> {


    private var properties: LightyProperties? = null
    private var bufferPool: Supplier<BufferPool>? = null

    fun properties(properties: LightyProperties): T {
        this.properties = properties
        return this as T
    }

    fun bufferPool(pool: Supplier<BufferPool>?): T {
        this.bufferPool = pool
        return this as T
    }

    companion object {
        val defaultProperties = DefaultLightyProperties()
    }

    private fun validate(lightyProperties: LightyProperties) {
        if (lightyProperties.bufferPoolMaxSize() < lightyProperties.readBufSize()) {
            throw LightyException("Illegal properties, readBufSize should less that bufferPoolMaxSize")
        }
    }

    protected fun buildConfiguration(): LightyConfiguration {
        if (properties == null) {
            properties = defaultProperties
        } else {
            validate(properties!!)
        }
        if (bufferPool == null) {
            bufferPool = Supplier<BufferPool> {
                LruBufferPool(DefaultByteBufferAllocator(), properties!!.bufferPoolMaxSize())
            }
        }

        return newConfiguration(properties!!, bufferPool!!)
    }

    private fun newConfiguration(
        lightyProperties: LightyProperties,
        bufferPool: Supplier<BufferPool>
    ): LightyConfiguration {
        return object : LightyConfiguration {
            override fun lettyProperties(): LightyProperties {
                return lightyProperties
            }

            override fun bufferPool(): Supplier<BufferPool> {
                return bufferPool
            }

        }
    }
}