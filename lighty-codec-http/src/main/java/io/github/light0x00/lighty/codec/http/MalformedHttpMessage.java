package io.github.light0x00.lighty.codec.http;

/**
 * @author light0x00
 * @since 2023/8/27
 */
public class MalformedHttpMessage extends RuntimeException {
    public MalformedHttpMessage(String message) {
        super(message);
    }
}
