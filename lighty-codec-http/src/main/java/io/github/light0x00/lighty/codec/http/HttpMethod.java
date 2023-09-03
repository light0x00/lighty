package io.github.light0x00.lighty.codec.http;

import lombok.Getter;

/**
 * @author light0x00
 * @since 2023/8/26
 */
public enum HttpMethod {
    GET(false), POST(true), PUT(true), DELETE(false), HEAD(false), OPTIONS(false);

    @Getter
    private boolean hasBody;

    HttpMethod(boolean hasBody) {
        this.hasBody = hasBody;
    }
}
