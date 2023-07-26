package io.github.light0x00.letty.core.handler;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface EventHandler {

    void onEvent(SelectionKey key) throws IOException;

}
