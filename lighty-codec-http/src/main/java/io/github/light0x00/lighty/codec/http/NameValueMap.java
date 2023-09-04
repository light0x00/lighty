package io.github.light0x00.lighty.codec.http;

import java.util.HashMap;

/**
 * @author light0x00
 * @since 2023/9/1
 */
public class NameValueMap extends HashMap<String, String> {

    public int getInt(String name, int def) {
        String s = get(name);
        return s == null ? def : Integer.parseInt(s);
    }
}
