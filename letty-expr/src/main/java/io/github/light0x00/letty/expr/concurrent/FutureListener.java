package io.github.light0x00.letty.expr.concurrent;


import io.github.light0x00.letty.expr.concurrent.ListenableFutureTask;

public interface FutureListener<T> {
    void operationComplete(ListenableFutureTask<T> futureTask) ;
}
