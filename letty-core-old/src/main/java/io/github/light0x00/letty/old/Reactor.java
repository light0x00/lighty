package io.github.light0x00.letty.old;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 * @author lightx00
 * @since 2022/3/12
 */
@Slf4j
@SuppressWarnings("Duplicates")
public class Reactor {

    private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();
    @SneakyThrows
    public Reactor() {
    }

    public void addTask(Runnable runnable) {
        tasks.offer(runnable);
    }

    @SneakyThrows
    public void eventLoop(Selector selector,Consumer<SelectionKey> handler) {
        while (!Thread.interrupted()) {
            Runnable c;
            while ((c = tasks.poll()) != null) {
                c.run();
            }
            selector.select();
            Set<SelectionKey> events = selector.selectedKeys();
            Iterator<SelectionKey> it = events.iterator();
            while (it.hasNext()) {
                SelectionKey event = it.next();
                handler.accept(event);
                it.remove();
            }
        }
    }

}
