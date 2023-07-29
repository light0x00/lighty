package io.github.light0x00.letty.old.expr;

import java.util.List;

public interface MessageHandler {

    List<ChannelPipeline> decodePipeline();

    List<ChannelPipeline> encodePipeline();

    void onOpen(EventContext context);

    void onMessage(EventContext context, Object msg);

    void onClose(EventContext context);

    void onError(Exception e, EventContext context);
}
