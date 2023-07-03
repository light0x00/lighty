package io.github.light0x00.letty.expr

import java.util.function.Function


/**
 * @author light0x00
 * @since 2023/6/28
 */
abstract class ChannelContext(
//    private val writeHandle: Function<Any,ListenableFutureTask<Void>>,
) {

    abstract fun write(data: Any) : ListenableFutureTask<Void>

    abstract fun close() : ListenableFutureTask<Void>

}