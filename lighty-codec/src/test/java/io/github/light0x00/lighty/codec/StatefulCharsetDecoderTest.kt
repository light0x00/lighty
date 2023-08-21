package io.github.light0x00.lighty.codec

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.charset.MalformedInputException
import java.nio.charset.StandardCharsets

/**
 * @author light0x00
 * @since 2023/8/20
 */
class StatefulCharsetDecoderTest {
    val decoder = StatefulCharsetDecoder(StandardCharsets.UTF_8.newDecoder())

    /**
     * 测试, 一个 4 字节字符分割为两次解码, 在第二次解码是否能正确解析
     */
    @Test
    fun test() {
        val decode1 = decoder.decode(
            ByteBuffer.wrap(
                byteArrayOf(
                    'S'.code.toByte(),
                    'a'.code.toByte(),
                    'k'.code.toByte(),
                    'u'.code.toByte(),
                    'r'.code.toByte(),
                    'a'.code.toByte(),
                    0xf0.toByte()
                )
            ),
            ByteBuffer.wrap(byteArrayOf(0x9f.toByte(), 0x8c.toByte()))
        )
        val decode2 = decoder.decode(ByteBuffer.wrap(byteArrayOf(0xb8.toByte())))

        Assertions.assertEquals("Sakura", decode1)
        Assertions.assertEquals("🌸", decode2)
    }

    /**
     * 测试在解码 “畸形”字节序列 时, 还能否继续解码正常字节序列
     */
    @Test
    fun test2() {
        Assertions.assertThrows(MalformedInputException::class.java) {
            decoder.decode(ByteBuffer.wrap(byteArrayOf(0x9f.toByte(), 0x8c.toByte())))
        }

        val decode = decoder.decode(
            ByteBuffer.wrap(
                byteArrayOf(
                    'S'.code.toByte(),
                    'a'.code.toByte(),
                    'k'.code.toByte(),
                    'u'.code.toByte(),
                    'r'.code.toByte(),
                    'a'.code.toByte(),
                    0xf0.toByte(), 0x9f.toByte(), 0x8c.toByte(), 0xb8.toByte()
                )
            )
        )
        Assertions.assertEquals("Sakura🌸", decode)
    }

    /**
     * 测试能否正常解码混合了不同编码长度的字符
     */
    @Test
    fun test3() {
        val decode1 = decoder.decode(
            ByteBuffer.wrap(
                byteArrayOf(
                    'S'.code.toByte(),
                    'a'.code.toByte(),
                    'k'.code.toByte(),
                    'u'.code.toByte(),
                    'r'.code.toByte(),
                    'a'.code.toByte(),
                    0xf0.toByte()
                )
            ),
            ByteBuffer.wrap(byteArrayOf(0x9f.toByte(), 0x8c.toByte())),
            ByteBuffer.wrap(byteArrayOf(0xb8.toByte()))
        )
        Assertions.assertEquals("Sakura🌸", decode1)
    }
}

