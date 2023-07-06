package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.ChannelContext;
import io.github.light0x00.letty.expr.ListenableFutureTask;
import io.github.light0x00.letty.expr.buffer.RecyclableByteBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;

public interface OutboundInvocation {

    static OutboundInvocation buildInvocationChain(ChannelContext context, List<OutboundChannelHandler> pipelines, BiConsumer<Object, ListenableFutureTask<Void>> receiver) {
        OutboundInvocation invocation = receiver::accept;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            OutboundChannelHandler pipeline = pipelines.get(i);

            OutboundInvocation next = invocation;
            invocation = (dataIn, future) -> pipeline.onWrite(
                    new ChannelContext() {
                        @NotNull
                        @Override
                        public ListenableFutureTask<Void> write(@NotNull Object data) {
                            next.invoke(data, future); //沿用这个 future , 好处会让上游也得到通知; 而如果重新 new, 那么上游的 addListener 将没有意义
                            return future;
                        }

                        @NotNull
                        @Override
                        public ListenableFutureTask<Void> close() {
                            return context.close();
                        }

                        @NotNull
                        @Override
                        public RecyclableByteBuffer allocateBuffer(int capacity) {
                            return context.allocateBuffer(capacity);
                        }
                    },
                    dataIn,
                    (dataOut) -> next.invoke(dataOut, future));
        }
        return invocation;
    }

    void invoke(Object arg, ListenableFutureTask<Void> future);

}
