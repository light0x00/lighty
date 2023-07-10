package io.github.light0x00.letty.expr.handler;

import io.github.light0x00.letty.expr.util.Skip;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.light0x00.letty.expr.util.Tool.existAnnotation;

/**
 * @author light0x00
 * @since 2023/7/10
 */
public class ChannelEventNotifier implements ChannelObserver {

    private final List<ChannelObserver> connectedEventObservers;

    private final List<ChannelObserver> readCompletedEventObservers;
    private final List<ChannelObserver> closedEventObservers;

    public ChannelEventNotifier(List<InboundChannelHandler> inboundPipelines, List<OutboundChannelHandler> outboundPipelines) {

        Set<ChannelObserver> observers = Stream.concat(inboundPipelines.stream(), outboundPipelines.stream())
                .collect(Collectors.toSet()); //去重,主要是针对同时实现了 inbound、outbound 接口的 handler

        connectedEventObservers = observers.stream().filter(
                it -> !existAnnotation(Skip.class, it.getClass(), "onConnected", ChannelContext.class)
        ).toList();

        readCompletedEventObservers = observers.stream().filter(
                it -> !existAnnotation(Skip.class, it.getClass(), "onReadCompleted", ChannelContext.class)
        ).toList();

        closedEventObservers = observers.stream().filter(
                it -> !existAnnotation(Skip.class, it.getClass(), "onClosed", ChannelContext.class)
        ).toList();

    }

    @Override
    public void onConnected(ChannelContext context) {
        for (ChannelObserver connectedEventObserver : connectedEventObservers) {
            connectedEventObserver.onConnected(context);
        }
    }

    @Override
    public void onReadCompleted(ChannelContext context) {
        for (ChannelObserver readCompletedEventObserver : readCompletedEventObservers) {
            readCompletedEventObserver.onReadCompleted(context);
        }
    }

    @Override
    public void onClosed(ChannelContext context) {
        for (ChannelObserver closedEventObserver : closedEventObservers) {
            closedEventObserver.onClosed(context);
        }
    }
//
//    @Override
//    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
//
//    }
//
//    @Override
//    public void onWrite(ChannelContext context, Object data, OutboundPipeline next) {
//
//    }

}
