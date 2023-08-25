package io.github.light0x00.lighty.core.concurrent

import java.util.function.Consumer

/**
 * @author light0x00
 * @since 2023/8/8
 */
class FailureFutureListener<T>(private val onFailure: Consumer<Throwable>) : FutureListener<T> {

    companion object {
        @JvmStatic
        fun <T> printStackTrace(): FailureFutureListener<T> {
            return FailureFutureListener { it.printStackTrace() }
        }
    }

    override fun operationComplete(future: ListenableFutureTask<T>) {
        if (!future.isSuccess) {
            onFailure.accept(future.cause())
        }
    }
}