package io.github.light0x00.letty.core.handler;

import io.github.light0x00.letty.core.handler.adapter.InboundChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.light0x00.letty.core.util.Tool.stackTraceToString;

public interface InboundPipelineInvocation {
    void invoke(Object arg);
}
