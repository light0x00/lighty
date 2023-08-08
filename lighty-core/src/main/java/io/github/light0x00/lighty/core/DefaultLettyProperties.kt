package io.github.light0x00.lighty.core

/**
 * @author light0x00
 * @since 2023/7/31
 */
open class DefaultLettyProperties : LettyProperties {
    override fun isAllowHalfClosure(): Boolean {
        return false
    }

    override fun readBufSize(): Int {
        return 1024
    }

    override fun bufferPoolMaxSize(): Int {
        return 128 * 1024 * 1024
    }
}