package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

import javax.annotation.Nonnull;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface OutboundPipeline {
    ListenableFutureTask<Void> invoke(@Nonnull Object data);

    ListenableFutureTask<Void> upstreamFuture();
}
