package io.github.light0x00.letty.expr;



public interface FutureListener<T> {
    void operationComplete(ListenableFutureTask<T> futureTask);
}
