package io.github.light0x00.letty.expr;

import java.nio.channels.SelectionKey;

public interface EventHandler {
    void onEvent(SelectionKey key);

}
