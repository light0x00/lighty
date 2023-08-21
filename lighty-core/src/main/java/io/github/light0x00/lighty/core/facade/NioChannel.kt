package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption

/**
 * @author light0x00
 * @since 2023/8/9
 */
interface NioChannel {

    fun close(): ListenableFutureTask<Void>

    fun localAddress(): SocketAddress

    fun <T> setOption(name: SocketOption<T>, value: T)

    fun <T> getOption(name: SocketOption<T>): T

}