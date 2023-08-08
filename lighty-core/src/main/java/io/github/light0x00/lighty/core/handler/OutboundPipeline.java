package io.github.light0x00.lighty.core.handler;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface OutboundPipeline {
    ListenableFutureTask<Void> invoke(Object data);
}
