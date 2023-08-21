package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

public interface InboundPipelineInvocation {
    void invoke(Object data, ListenableFutureTask<Void> upstreamFuture);
}
