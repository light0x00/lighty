package io.github.light0x00.letty.core.concurrent;

import io.github.light0x00.letty.core.handler.NioEventHandler;

import java.nio.channels.SelectionKey;

/**
 * @author light0x00
 * @since 2023/8/8
 */
public interface EventHandlerProvider<T extends NioEventHandler> {
    T get(SelectionKey key);
}
