package io.github.light0x00.lighty.codec.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/9/1
 */
@AllArgsConstructor
@Getter
public enum ResponseStatus {

    OK(200, "OK",String.valueOf(200).getBytes(),"OK".getBytes()),
    NOT_FOUND(404, "NOT_FOUND",
            String.valueOf(200).getBytes(StandardCharsets.ISO_8859_1),
            "NOT_FOUND".getBytes());

    private final int code;
    private final String text;
    private final byte[] codeBytes;
    private final byte[] textBytes;
}
