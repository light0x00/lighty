package io.github.light0x00.letty.core.handler;

/**
 * @author light0x00
 * @since 2023/7/4
 */
public interface InboundPipeline {

    void invoke(Object data);

}
