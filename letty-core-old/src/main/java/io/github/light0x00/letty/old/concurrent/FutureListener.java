package io.github.light0x00.letty.old.concurrent;


public interface FutureListener<T> {
    void operationComplete(ListenableFutureTask<T> futureTask);
}
