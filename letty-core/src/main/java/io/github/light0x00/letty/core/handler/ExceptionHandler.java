package io.github.light0x00.letty.core.handler;

/**
 * @author light0x00
 * @since 2023/8/2
 */
public interface ExceptionHandler {

    void onException(Throwable throwable);

}
