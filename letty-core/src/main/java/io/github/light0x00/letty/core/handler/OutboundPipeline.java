package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface OutboundPipeline {
    ListenableFutureTask<Void> invoke(Object data);
}
