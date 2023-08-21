package io.github.light0x00.lighty.codec;

import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * @author light0x00
 * @since 2023/8/19
 */
public class CharsetDecoderTest {

    @SneakyThrows
    public static void main(String[] args) {
        //The utf-8 bytes sequences that we'll decode it
        ByteBuffer byteSequence = ByteBuffer.wrap(
                "hello😄Привет Hello 你好 こんにちは 안녕하세요,😂".getBytes(StandardCharsets.UTF_8)
        );


        StringBuilder decodeResult = new StringBuilder();

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        ByteBuffer decodeBufIn = ByteBuffer.allocate(4);
        CharBuffer decodeBufOut = CharBuffer.allocate(2);

        // Due to the awful design of ByteBuffer, we need to maintain write position ourself
        int writePosition = 0;

        // Decode byte by byte
        while (byteSequence.remaining() > 0) {
            decodeBufIn.put(writePosition++, byteSequence.get());

            //Switch to read mode
            decodeBufIn.limit(writePosition);
            CoderResult r = decoder.decode(decodeBufIn, decodeBufOut, false);

            //Once the decoder produce an outcome , consume it
            if (r.isUnderflow() || r.isOverflow()) {
                if (decodeBufOut.position() > 0) {
                    decodeBufOut.flip();
                    decodeResult.append(decodeBufOut);
                    decodeBufOut.clear();

                    decodeBufIn.clear();
                    writePosition = 0;

                    System.out.println(decodeResult);
                }
            } else {
                r.throwException();
            }

            //Switch to write mode
            decodeBufIn.limit(decodeBufIn.capacity());

            if (writePosition >= decodeBufIn.capacity()) {
                throw new IllegalStateException("This should never occur!");
            }
        }

        System.out.println(decodeResult);
    }
}
