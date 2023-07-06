package io.github.light0x00.letty.expr.handler;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface OutboundPipeline {
    void invoke(Object data);
}
