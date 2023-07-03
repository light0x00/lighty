package io.github.light0x00.letty.expr;

/**
 * @author light0x00
 * @since 2023/7/1
 */
public interface ChannelObserver {
    //TODO 默认实现移到适配器类

    default void onConnected(ChannelContext context) {
    }

    default void onReadCompleted(ChannelContext context) {
    }

    default void onClosed(ChannelContext context) {
    }

}
