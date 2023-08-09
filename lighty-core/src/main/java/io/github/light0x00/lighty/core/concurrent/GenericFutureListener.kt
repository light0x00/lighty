package io.github.light0x00.lighty.core.concurrent

/**
 * @author light0x00
 * @since 2023/8/8
 */
abstract class GenericFutureListener<T> : FutureListener<T> {

    override fun operationComplete(future: ListenableFutureTask<T>) {
        if (future.isSuccess) {
            onSuccess(future.get())
        } else {
            onFailure(future.cause())
        }
    }

    abstract fun onSuccess(result: T)

    abstract fun onFailure(cause: Throwable)

}
