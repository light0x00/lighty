package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.eventloop.EventLoop;
import io.github.light0x00.letty.core.util.Skip;
import io.github.light0x00.letty.core.util.Tool;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author light0x00
 * @since 2023/7/10
 */
public class ChannelEventNotifier implements ChannelObserver {

    private final List<ChannelObserver> connectedEventObservers;

    private final List<ChannelObserver> readCompletedEventObservers;
    private final List<ChannelObserver> closedEventObservers;

    /**
     * The event loop the observers are executed in
     */
    EventLoop eventLoop;

    public ChannelEventNotifier(
            EventLoop eventLoop,
            List<InboundChannelHandler> inboundPipelines,
            List<OutboundChannelHandler> outboundPipelines) {

        this.eventLoop = eventLoop;

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

    }

    @Override
    public void onConnected(ChannelContext context) {
        for (ChannelObserver connectedEventObserver : connectedEventObservers) {
            if (eventLoop.inEventLoop()) {
                connectedEventObserver.onConnected(context);
            } else {
                eventLoop.execute(() -> connectedEventObserver.onConnected(context));
            }
        }
    }

    @Override
    public void onReadCompleted(ChannelContext context) {
        for (ChannelObserver readCompletedEventObserver : readCompletedEventObservers) {
            if (eventLoop.inEventLoop()) {
                readCompletedEventObserver.onReadCompleted(context);
            } else {
                eventLoop.execute(() -> readCompletedEventObserver.onReadCompleted(context));
            }
        }
    }

    @Override
    public void onClosed(ChannelContext context) {
        for (ChannelObserver closedEventObserver : closedEventObservers) {
            if (eventLoop.inEventLoop()) {
                closedEventObserver.onClosed(context);
            } else {
                eventLoop.execute(() -> closedEventObserver.onClosed(context));
            }
        }
    }

}
