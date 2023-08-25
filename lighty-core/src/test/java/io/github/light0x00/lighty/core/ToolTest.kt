package io.github.light0x00.lighty.core

import io.github.light0x00.lighty.core.util.Tool.bytesToLong
import io.github.light0x00.lighty.core.util.Tool.longToBytes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * @author light0x00
 * @since 2023/8/24
 */
class ToolTest {

    @Test
    fun testConvertBetweenBytesAndLong() {
        Assertions.assertEquals(Long.MAX_VALUE, bytesToLong(longToBytes(Long.MAX_VALUE)));
        Assertions.assertEquals(Long.MIN_VALUE, bytesToLong(longToBytes(Long.MIN_VALUE)));
    }
}