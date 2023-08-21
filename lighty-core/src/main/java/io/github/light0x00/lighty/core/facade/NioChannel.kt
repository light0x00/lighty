package io.github.light0x00.lighty.core.facade

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask
import java.net.SocketAddress
import java.net.SocketOption

/**
 * @author light0x00
 * @since 2023/8/9
 */
interface NioChannel {

    /**
     * Close the channel.
     */
    fun close(): ListenableFutureTask<Void>

    /**
     * @see [java.nio.channels.NetworkChannel.getLocalAddress]
     */
    fun localAddress(): SocketAddress

    /**
     * @see [java.nio.channels.NetworkChannel.setOption]
     */
    fun <T> setOption(name: SocketOption<T>, value: T)

    /**
     * @see [java.nio.channels.NetworkChannel.getOption]
     */
    fun <T> getOption(name: SocketOption<T>): T

}