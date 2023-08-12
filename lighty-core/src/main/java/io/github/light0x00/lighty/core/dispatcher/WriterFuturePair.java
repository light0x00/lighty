package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.lighty.core.dispatcher.writestrategy.WriteStrategy;

/**
 * @author light0x00
 * @since 2023/8/12
 */
public record WriterFuturePair(WriteStrategy writer, ListenableFutureTask<Void> future) {

}
