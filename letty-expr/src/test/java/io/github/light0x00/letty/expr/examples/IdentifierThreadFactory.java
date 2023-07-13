package io.github.light0x00.letty.expr.examples;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author light0x00
 * @since 2023/7/13
 */
public class IdentifierThreadFactory implements ThreadFactory {

    String identifier;
    AtomicInteger id = new AtomicInteger();

    public IdentifierThreadFactory(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Thread newThread(@Nonnull Runnable r) {
        return new Thread(r, identifier + "-" + id.getAndIncrement());
    }
}
