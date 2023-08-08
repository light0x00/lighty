package io.github.light0x00.lighty.core.concurrent

import java.util.function.Consumer

/**
 * @author light0x00
 * @since 2023/8/8
 */
class SuccessFutureListener<T>(private val onSuccess: Consumer<T>) : FutureListener<T> {

    override fun operationComplete(future: ListenableFutureTask<T>) {
        if (future.isSuccess) {
            onSuccess.accept(future.get())
        }
    }
}
