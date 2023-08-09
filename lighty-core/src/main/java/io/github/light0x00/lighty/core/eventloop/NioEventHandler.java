package io.github.light0x00.lighty.core.eventloop;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface NioEventHandler {

    void onEvent(SelectionKey key) throws IOException;

    ListenableFutureTask<Void> close();

}
