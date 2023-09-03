package io.github.light0x00.lighty.codec.http;

/**
 * @author light0x00
 * @since 2023/9/2
 */
public class HttpConstants {

    /**
     * Horizontal space
     */
    public static final byte SP =32;

    /**
     * Carriage return
     */
    public static final byte CR = 13;

    /**
     * Line feed character
     */
    public static final byte LF = 10;

    public static final byte[] CRLF = new byte[]{CR,LF};

    /**
     * Colon ':'
     */
    public static final byte COLON = 58;

    public static final byte[] COLON_SP = new byte[]{COLON, SP};
}
