package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.buffer.RecyclableBuffer;
import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventExecutor;
import io.github.light0x00.letty.core.util.Skip;
import io.github.light0x00.letty.core.util.Tool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.light0x00.letty.core.util.Tool.stackTraceToString;

/**
 * @author light0x00
 * @since 2023/8/2
 */
@Slf4j
public class ChannelHandlerDispatcher {
    InboundPipelineInvocation inboundChain;

    OutboundPipelineInvocation outboundChain;

    private final List<ChannelObserver> connectedEventObservers;

    private final List<ChannelObserver> readCompletedEventObservers;

    private final List<ChannelObserver> closedEventObservers;

    /**
     * Triggered when the connection established successfully
     */
    @Getter
    protected final ListenableFutureTask<Void> connectedFuture;
    /**
     * Triggered when the connection closed.
     */
    @Getter
    protected final ListenableFutureTask<Void> closedFuture;

    EventExecutor eventExecutor;

    public ChannelHandlerDispatcher(EventExecutor eventExecutor,
                                    ChannelContext context,
                                    List<InboundChannelHandler> inboundHandlers,
                                    List<OutboundChannelHandler> outboundHandlers,
                                    OutboundPipelineInvocation receiver
    ) {
        this.eventExecutor = eventExecutor;
        this.connectedFuture = new ListenableFutureTask<>(null);
        this.closedFuture = new ListenableFutureTask<>(null);

        Set<ChannelObserver> observers = Stream.concat(inboundHandlers.stream(), outboundHandlers.stream())
                .collect(Collectors.toSet()); //去重,主要是针对同时实现了 inbound、outbound 接口的 handler

        inboundHandlers = filter(inboundHandlers, ha ->
                !InboundChannelHandler.getOnReadMethod(ha).isAnnotationPresent(Skip.class));

        outboundHandlers = filter(outboundHandlers, ha ->
                !OutboundChannelHandler.getMethodOnWrite(ha).isAnnotationPresent(Skip.class));

        inboundChain = InboundPipelineInvocation.buildInvocationChain(
                context, inboundHandlers, arg -> {
                    //the last phase
                });
        outboundChain = OutboundPipelineInvocation.buildInvocationChain(
                context, outboundHandlers, receiver);

        connectedEventObservers = observers.stream().filter(
                it -> !Tool.getMethod(it, "onConnected", ChannelContext.class)
                        .isAnnotationPresent(Skip.class)
        ).toList();

        readCompletedEventObservers = observers.stream().filter(
                it -> !Tool.getMethod(it, "onReadCompleted", ChannelContext.class)
                        .isAnnotationPresent(Skip.class)
        ).toList();

        closedEventObservers = observers.stream().filter(
                it -> !Tool.getMethod(it, "onClosed", ChannelContext.class)
                        .isAnnotationPresent(Skip.class)
        ).toList();

    }

    private void run(Runnable runnable) {
        if (eventExecutor.inEventLoop()) {
            runnable.run();
        } else {
            eventExecutor.execute(runnable);
        }
    }

    public void onConnected(ChannelContext context) {
        run(() -> {
            for (ChannelObserver observer : connectedEventObservers) {
                try {
                    observer.onConnected(context);
                } catch (Throwable throwable) {
                    invokeExceptionCaught(observer, context, throwable);
                }
            }
            connectedFuture.setSuccess();
        });
    }

    public void onReadCompleted(ChannelContext context) {
        run(() -> {
            for (ChannelObserver observer : readCompletedEventObservers) {
                try {
                    observer.onReadCompleted(context);
                } catch (Throwable throwable) {
                    invokeExceptionCaught(observer, context, throwable);
                }
            }
        });
    }

    public void onClosed(ChannelContext context) {
        run(() -> {
            for (ChannelObserver observer : closedEventObservers) {
                try {
                    observer.onClosed(context);
                } catch (Throwable throwable) {
                    invokeExceptionCaught(observer, context, throwable);
                }
            }
            closedFuture.setSuccess();
        });
    }

    public void input(RecyclableBuffer buf) {
        if (eventExecutor.inEventLoop()) {
            inboundChain.invoke(buf);
        } else {
            eventExecutor.execute(() -> inboundChain.invoke(buf));
        }
    }

    public ListenableFutureTask<Void> output(Object data) {
        var writeFuture = new ListenableFutureTask<Void>(null);
        if (eventExecutor.inEventLoop()) {
            outboundChain.invoke(data, writeFuture);
        } else {
            eventExecutor.execute(() -> outboundChain.invoke(data, writeFuture));
        }
        return writeFuture;
    }

    private static <T> List<T> filter(List<T> lst, Predicate<T> predicate) {
        return lst.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    static void invokeExceptionCaught(ChannelObserver observer, ChannelContext context, Throwable cause) {
        try {
            observer.exceptionCaught(context, cause);
        } catch (Throwable error) {
            log.warn("""
                            An exception {} was thrown by a user handler's exceptionCaught() method while handling the following exception:"""
                    , stackTraceToString(error), cause
            );
        }
    }
}
