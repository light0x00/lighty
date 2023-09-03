/**
 * @author light0x00
 * @since 2023/8/9
 */
module lighty.codec.http {
    requires static jsr305;
    requires org.slf4j;
    requires lighty.core;
    requires lighty.codec;
    requires static lombok;
    requires ch.qos.logback.classic;

    exports io.github.light0x00.lighty.codec.http;
}