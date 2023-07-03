package io.github.light0x00.letty.expr;

import java.nio.channels.SelectionKey;

public interface ChannelHandler {
    void onEvent(SelectionKey key);

}
