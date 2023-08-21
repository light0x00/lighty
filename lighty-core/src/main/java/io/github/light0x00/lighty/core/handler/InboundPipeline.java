package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface InboundPipeline {

    ListenableFutureTask<Void> next(Object data);

    ListenableFutureTask<Void> upstreamFuture();
}
