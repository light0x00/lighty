package io.github.light0x00.letty.expr;

/**
 * @author light0x00
 * @since 2023/7/1
 */
public interface ChannelPipeline {

    default void onConnected(ChannelContext context, Invocation next) {
    }

    default void onReadEOF(ChannelContext context, Invocation next) {
    }

    default void onClosed(ChannelContext context, Invocation next) {
    }

}
