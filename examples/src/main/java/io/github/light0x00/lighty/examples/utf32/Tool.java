package io.github.light0x00.lighty.examples.utf32;

/**
 * @author light0x00
 * @since 2023/8/9
 */
public class Tool {
    public static byte[] intToBytes(int i) {
        return new byte[]{
                (byte) (i >>> 24),
                (byte) (i >>> 16),
                (byte) (i >>> 8),
                (byte) i
        };
    }

    public static int bytesToInt(byte[] b) {
        //Cuz int encode by complement-on-two
        //For a negative, signed left shift operation will Fill the upper part of the binary with 1.
        //That's a question for us to combine the meaningful part.

        //Here, we execute a AND 0xFF operation, to implicitly convert a byte to int, and fill  the upper part of the binary with 0
        //So ,we got a positive number now.
        //The next step just execute OR operation to combine the four part as an integer.
        return b[0] << 24 |
                (b[1] & 0xFF) << 16 |
                (b[2] & 0xFF) << 8 |
                (b[3] & 0xFF);
    }
}
