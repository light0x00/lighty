/**
 * @author light0x00
 * @since 2023/8/9
 */
module io.github.light0x00.lighty.core {
    requires kotlin.stdlib;
    requires jsr305;
    requires lombok;
    requires org.slf4j;

    exports io.github.light0x00.lighty.core.eventloop;
    exports io.github.light0x00.lighty.core.buffer;
    exports io.github.light0x00.lighty.core.handler;
    exports io.github.light0x00.lighty.core.concurrent;
    exports io.github.light0x00.lighty.core.facade;
}