package io.github.light0x00.letty.core

/**
 * @author light0x00
 * @since 2023/7/31
 */
class DefaultLettyProperties : LettyProperties {
    override fun isAllowHalfClosure(): Boolean {
        return false
    }

    override fun readBufSize(): Int {
        return 16
    }

    override fun bufferPoolMaxSize(): Int {
        return 64
    }
}