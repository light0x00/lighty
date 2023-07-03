package io.github.light0x00.letty.expr;

/**
 * @author light0x00
 * @since 2023/6/30
 */
public interface MessageHandler {

    void onOpen(ChannelContext context);

    void onMessage(ChannelContext context, Object msg);

    void onClose(ChannelContext context);

    void onError(Exception e, ChannelContext context);

}
