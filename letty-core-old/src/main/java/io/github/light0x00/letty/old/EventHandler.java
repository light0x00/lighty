package io.github.light0x00.letty.old;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * @author lightx00
 * @since 2023/6/16
 */
interface EventHandler {
    void onAcceptable(SelectableChannel channel, SelectionKey key);

    void onReadable(SelectableChannel channel, SelectionKey key);

    void onWriteable(SelectableChannel channel, SelectionKey key);

    void onConnectable(SelectableChannel channel, SelectionKey key);
}
