package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.concurrent.ListenableFutureTask;
import io.github.light0x00.letty.core.eventloop.EventExecutor;
import io.github.light0x00.letty.core.util.Skip;
import io.github.light0x00.letty.core.util.Tool;
import lombok.Getter;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author light0x00
 * @since 2023/7/10
 */
@Immutable
public class ChannelEventNotifier implements ChannelObserver {

    private final List<ChannelObserver> connectedEventObservers;

    private final List<ChannelObserver> readCompletedEventObservers;
    private final List<ChannelObserver> closedEventObservers;
    private final List<ChannelObserver> errorEventObservers;

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

    /**
     * The event loop the observers should be executed in
     */
    private final EventExecutor eventExecutor;

    public ChannelEventNotifier(
            EventExecutor eventExecutor,
            List<InboundChannelHandler> inboundPipelines,
            List<OutboundChannelHandler> outboundPipelines) {

        this.eventExecutor = eventExecutor;
        this.connectedFuture = new ListenableFutureTask<>(null);
        this.closedFuture = new ListenableFutureTask<>(null);

        Set<ChannelObserver> observers = Stream.concat(inboundPipelines.stream(), outboundPipelines.stream())
                .collect(Collectors.toSet()); //去重,主要是针对同时实现了 inbound、outbound 接口的 handler

        connectedEventObservers = observers.stream().filter(
                it -> !Tool.methodExistAnnotation(Skip.class, it.getClass(), "onConnected", ChannelContext.class)
        ).toList();

        readCompletedEventObservers = observers.stream().filter(
                it -> !Tool.methodExistAnnotation(Skip.class, it.getClass(), "onReadCompleted", ChannelContext.class)
        ).toList();

        closedEventObservers = observers.stream().filter(
                it -> !Tool.methodExistAnnotation(Skip.class, it.getClass(), "onClosed", ChannelContext.class)
        ).toList();

        errorEventObservers = observers.stream().filter(
                it -> !Tool.methodExistAnnotation(Skip.class, it.getClass(), "onError", ChannelContext.class, Throwable.class)
        ).toList();
    }

    @Override
    public void exceptionCaught(ChannelContext context, Throwable th) {
        if (eventExecutor.inEventLoop()) {
            for (ChannelObserver errorEventObserver : errorEventObservers) {
                errorEventObserver.exceptionCaught(context, th);
            }
        } else {
            for (ChannelObserver errorEventObserver : errorEventObservers) {
                eventExecutor.execute(() -> errorEventObserver.exceptionCaught(context, th));
            }
        }
    }

    @Override
    public void onConnected(ChannelContext context) {
        if (eventExecutor.inEventLoop()) {
            connectedFuture.setSuccess();
            for (ChannelObserver connectedEventObserver : connectedEventObservers) {
                connectedEventObserver.onConnected(context);
            }
        } else {
            eventExecutor.execute(connectedFuture::setSuccess);
            for (ChannelObserver connectedEventObserver : connectedEventObservers) {
                eventExecutor.execute(() -> connectedEventObserver.onConnected(context));
            }
        }
    }

    @Override
    public void onReadCompleted(ChannelContext context) {
        if (eventExecutor.inEventLoop()) {
            for (ChannelObserver readCompletedEventObserver : readCompletedEventObservers) {
                readCompletedEventObserver.onReadCompleted(context);
            }
        } else {
            for (ChannelObserver readCompletedEventObserver : readCompletedEventObservers) {
                eventExecutor.execute(() -> readCompletedEventObserver.onReadCompleted(context));
            }
        }
    }

    @Override
    public void onClosed(ChannelContext context) {
        if (eventExecutor.inEventLoop()) {
            closedFuture.setSuccess();
            for (ChannelObserver closedEventObserver : closedEventObservers) {
                closedEventObserver.onClosed(context);
            }
        } else {
            eventExecutor.execute(closedFuture::setSuccess);
            for (ChannelObserver closedEventObserver : closedEventObservers) {
                eventExecutor.execute(() -> closedEventObserver.onClosed(context));
            }
        }
    }

}
