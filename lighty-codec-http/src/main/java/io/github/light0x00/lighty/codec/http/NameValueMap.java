package io.github.light0x00.lighty.codec.http;

import java.util.HashMap;

/**
 * @author light0x00
 * @since 2023/9/1
 */
public class NameValueMap extends HashMap<String, String> {

    public Integer getInt(String name) {
        String s = get(name);
        return s == null ? null : Integer.valueOf(s);
    }
}
