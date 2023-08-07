package io.github.light0x00.letty.core.concurrent;


public interface FutureListener<T> {
    void operationComplete(ListenableFutureTask<T> future);
}
