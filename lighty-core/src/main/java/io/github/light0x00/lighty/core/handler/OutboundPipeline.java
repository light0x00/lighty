package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.facade.NioSocketChannel;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface OutboundPipeline {

    /**
     * <p>
     * Pass data to next phase of pipeline.
     * It's similar to:
     *
     * <li>{@link NioSocketChannel#write(Object)}
     * <li>{@link NioSocketChannel#writeAndFlush(Object)}
     *
     * <p>
     * The only difference is that:
     *
     * <p>
     * Weather or not {@link #next(Object)} flush the data, depends on the upstream.
     * If the upstream call {@link NioSocketChannel#writeAndFlush(Object)},
     * it flush, otherwise not.
     *
     * <p>
     * If you want to decide whether flush or not,
     * use directly {@link NioSocketChannel#write(Object)} or {@link NioSocketChannel#writeAndFlush(Object)} (Object)}
     */
    ListenableFutureTask<Void> next(@Nonnull Object data);

    ListenableFutureTask<Void> upstreamFuture();
}
