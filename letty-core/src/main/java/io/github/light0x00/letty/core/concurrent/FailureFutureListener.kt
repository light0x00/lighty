package io.github.light0x00.letty.core.concurrent

import java.util.function.Consumer

/**
 * @author light0x00
 * @since 2023/8/8
 */
class FailureFutureListener(private val onFailure: Consumer<Throwable>) : FutureListener<Any> {
    override fun operationComplete(future: ListenableFutureTask<Any>) {
        if (!future.isSuccess) {
            onFailure.accept(future.cause())
        }
    }
}