package io.github.light0x00.letty.core.concurrent;


import javax.annotation.Nonnull;

public interface FutureListener<T> {
    void operationComplete(@Nonnull ListenableFutureTask<T> future);
}
