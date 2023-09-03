package io.github.light0x00.lighty.codec.http;

import lombok.Data;

/**
 * @author light0x00
 * @since 2023/8/26
 */
@Data
public class HttpResponse {

    private String version;
    private ResponseStatus status;
    private NameValueMap headers = new NameValueMap();
    private byte[] body;

}
