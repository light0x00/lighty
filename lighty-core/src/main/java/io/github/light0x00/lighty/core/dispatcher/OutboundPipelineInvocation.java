package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;

public interface OutboundPipelineInvocation {

    /**
     * @param data   the data that transfer to next phase of pipeline
     * @param future the future that will complete when the data actually write to socket send buffer
     * @param flush if true the data will be written as soon as the socket is writeable
     */
    void invoke(Object data, ListenableFutureTask<Void> future, boolean flush);

}
