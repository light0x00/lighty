package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface NioEventHandler {

    void onEvent(SelectionKey key) throws IOException;

    ListenableFutureTask<Void> close();

}
