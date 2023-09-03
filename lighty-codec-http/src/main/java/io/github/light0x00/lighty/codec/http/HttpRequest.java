package io.github.light0x00.lighty.codec.http;

import lombok.Data;

/**
 * @author light0x00
 * @since 2023/8/26
 */
@Data
public class HttpRequest {
    private HttpMethod method;
    private String requestPath;
    private String version;
    private NameValueMap headers = new NameValueMap();
    private byte[] body;

}
