package io.github.light0x00.lighty.codec;

import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * @author light0x00
 * @since 2023/8/20
 */
public class StatefulCharsetDecoder {

    CharsetDecoder decoder;

    StringBuilder stringBuilder = new StringBuilder();

    ByteBuffer decodeBuf = ByteBuffer.allocate(4);

    CharBuffer decodeOutcome = CharBuffer.allocate(2);//utf-16 at most 2 code unit (4 bytes)

    int decodeBufWritePosition = 0;

    public StatefulCharsetDecoder(Charset charset) {
        this(charset.newDecoder());
    }

    public StatefulCharsetDecoder(CharsetDecoder decoder) {
        this.decoder = decoder;
    }

    @SneakyThrows
    public String decode(ByteBuffer... srcBuffers) {
        for (ByteBuffer src : srcBuffers) {
            int srcReadPosition = src.position();
            int srcReadRemaining = src.limit() - srcReadPosition;

            while (srcReadRemaining > 0) {
                //1. load the bytes to decode buffer
                int loadNum = Math.min(srcReadRemaining, decodeBuf.capacity() - decodeBufWritePosition);
                loadBytes(src, srcReadPosition, loadNum);

                srcReadPosition += loadNum;
                srcReadRemaining -= loadNum;

                //2. decode the bytes
                decode0();
            }
        }
        String str = stringBuilder.toString();
        stringBuilder.setLength(0);
        return str;
    }

    private void loadBytes(ByteBuffer src, int offset, int length) {
        decodeBuf.limit(decodeBuf.capacity());
        decodeBuf.put(decodeBufWritePosition, src, offset, length);
        decodeBufWritePosition += length;
    }

    private void decode0() throws CharacterCodingException {
        CoderResult r;
        do {
            //切换到读模式
            decodeBuf.limit(decodeBufWritePosition);
            r = decoder.decode(decodeBuf, decodeOutcome, false);

            if (r.isUnderflow() || r.isOverflow()) {

                int unreadLength = decodeBuf.remaining();
                if (decodeBuf.position() > 0 && unreadLength > 0) {
                    decodeBuf.compact(); //始终保证未读数据与数组 0 位置对齐
                }
                decodeBuf.position(0);
                decodeBufWritePosition = unreadLength;

                if (decodeOutcome.position() > 0) {
                    decodeOutcome.flip();
                    stringBuilder.append(decodeOutcome);
                    decodeOutcome.clear();
                }

            } else {
                decodeOutcome.clear();
                decodeBuf.clear();
                decodeBufWritePosition = 0;

                r.throwException();
            }
        }
        // 说明 decodeBuf 还有剩余可解码数据, 只是因为 decodeOutcome 太小一次装不下
        while (r.isOverflow());
    }
}
