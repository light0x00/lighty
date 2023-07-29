package io.github.light0x00.letty.old.expr;

import java.util.List;
import java.util.function.Consumer;

interface Invocation {

    void invoke(Object arg);

    static Invocation buildInvocationChain(EventContext context, List<ChannelPipeline> pipelines, Consumer<Object> receiver) {
        Invocation invocation = receiver::accept;

        for (int i = pipelines.size() - 1; i >= 0; i--) {
            ChannelPipeline pipeline = pipelines.get(i);

            Invocation next = invocation;
            invocation = data -> pipeline.launch(context, data, next);
        }
        return invocation;
    }
}
