package io.github.light0x00.lighty.core.eventloop;

import io.github.light0x00.lighty.core.dispatcher.NioEventHandler;

import java.nio.channels.SelectionKey;

/**
 * @author light0x00
 * @since 2023/8/8
 */
public interface EventHandlerProvider<T extends NioEventHandler> {
    T get(SelectionKey key);
}
