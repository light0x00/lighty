package io.github.light0x00.lighty.core

import io.github.light0x00.lighty.core.buffer.RingBuffer
import io.github.light0x00.lighty.core.buffer.RingBufferImpl
import io.github.light0x00.lighty.core.util.Tool
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

/**
 * @author light0x00
 * @since 2023/7/6
 */
class RingByteBufferTest {

    @Test
    fun testConstruct() {
        val buf = ByteBuffer.allocate(16)
        buf.put(1)
        buf.put(2)
        buf.put(3)
        buf.flip()

        val rBuf = RingBufferImpl(
            buf,
            buf.position(),
            buf.limit(),
            buf.capacity(),
            false
        )
        for (i in 1..3) {
            Assertions.assertEquals(i.toByte(), rBuf.get())
        }
        Assertions.assertThrows(BufferUnderflowException::class.java) {
            rBuf.get()
        }
    }

    @Test
    fun testConstruct2() {
        val buf = ByteBuffer.allocate(16)
        buf.put(1)
        buf.put(2)
        buf.put(3)
        buf.put(4)
        buf.flip()

        val rBuf = RingBufferImpl(buf, 3, 1, 4, false)

        Assertions.assertEquals(2, rBuf.remainingCanGet())

        Assertions.assertEquals(4, rBuf.get())
        Assertions.assertEquals(1, rBuf.get())
        Assertions.assertThrows(BufferUnderflowException::class.java) {
            rBuf.get()
        }
    }

    @Test
    fun testGetWhatPut() {
        val buf = RingBufferImpl(ByteBuffer.allocate(4))
        buf.put(1.toByte())
        buf.put(3.toByte())
        buf.put(5.toByte())
        Assertions.assertEquals(1.toByte(), buf.get())
        Assertions.assertEquals(3.toByte(), buf.get())
        Assertions.assertEquals(5.toByte(), buf.get())
    }

    @Test
    fun testGetWhatPut2() {
        val buf = RingBufferImpl(ByteBuffer.allocate(4))
        val cp = "😅".codePoints().findFirst().asInt
        println(Character.toString(cp))
        buf.put(Tool.intToBytes(cp))
        val cpb = ByteArray(4)
        buf[cpb]
        Assertions.assertEquals(
            "😅",
            Character.toString(Tool.bytesToInt(cpb))
        )
    }

    @Test
    fun testPutOverflow() {
        val buf = RingBufferImpl(ByteBuffer.allocate(4))
        Assertions.assertThrows(
            BufferOverflowException::class.java
        ) { buf.put(byteArrayOf(0, 1, 2, 3, 4)) }
    }

    @Test
    fun testGetUnderflow() {
        val buf = RingBufferImpl(ByteBuffer.allocate(4))
        buf.put(byteArrayOf(1, 2))
        Assertions.assertThrows(BufferUnderflowException::class.java) {
            buf.get(ByteArray(3))
        }
    }

    /**
     * 轮流执行 put 、 get
     */
    @Test
    fun testPutAndGetAlternately() {
        val buf = RingBufferImpl(ByteBuffer.allocate(4))
        /*
         ┌───┬───┬───┬───┐
         │ R │   │ W │   │
         └───┴───┴───┴───┘
         */
        buf.put(byteArrayOf(1, 2))

        /*
         ┌───┬───┬───┬───┐
         │   │ R │ W │   │
         └───┴───┴───┴───┘
         */
        Assertions.assertEquals(1.toByte(), buf.get())
        Assertions.assertEquals(1, buf.remainingCanGet())
        Assertions.assertEquals(3, buf.remainingCanPut())
        Assertions.assertThrows(
            BufferOverflowException::class.java
        ) { buf.put(byteArrayOf(3, 4, 5, 6)) }

        /*
          ┌───┬───┬───┬───┐
          │   │W/R│   │   │
          └───┴───┴───┴───┘
          */
        buf.put(byteArrayOf(3, 4, 5), 0, 3)
        Assertions.assertEquals(4, buf.remainingCanGet())
        Assertions.assertEquals(0, buf.remainingCanPut())
        Assertions.assertThrows(BufferOverflowException::class.java) {
            buf.put(
                7.toByte()
            )
        }

        /*
          ┌───┬───┬───┬───┐
          │   │R/W│   │   │
          └───┴───┴───┴───┘
          */
        val readBytes = ByteArray(4)
        buf[readBytes]
        Assertions.assertArrayEquals(byteArrayOf(2, 3, 4, 5), readBytes)
        Assertions.assertEquals(0, buf.remainingCanGet())
        Assertions.assertEquals(4, buf.remainingCanPut())
    }

    @Test
    fun testPutAndGetAlternately2() {
        val buf = RingBufferImpl(ByteBuffer.allocate(4))
        /*
         ┌───┬───┬───┬───┐
         │ R │   │ W │   │
         └───┴───┴───┴───┘
        */
        buf.put(ByteBuffer.wrap(byteArrayOf(1, 2)))
        Assertions.assertEquals(2, buf.remainingCanGet())
        Assertions.assertEquals(2, buf.remainingCanPut())

        /*
         ┌───┬───┬───┬───┐
         │   │ R │ W │   │
         └───┴───┴───┴───┘
        */
        Assertions.assertEquals(1.toByte(), buf.get())
        Assertions.assertEquals(1, buf.remainingCanGet())
        Assertions.assertEquals(3, buf.remainingCanPut())
        Assertions.assertThrows(
            BufferOverflowException::class.java
        ) { buf.put(ByteBuffer.wrap(byteArrayOf(3, 4, 5, 6))) }

        /*
         ┌───┬───┬───┬───┐
         │   │W/R│   │   │
         └───┴───┴───┴───┘
         */buf.put(byteArrayOf(3, 4, 5, 6), 0, 3)
        Assertions.assertEquals(4, buf.remainingCanGet())
        Assertions.assertEquals(0, buf.remainingCanPut())
        Assertions.assertThrows(BufferOverflowException::class.java) {
            buf.put(
                ByteBuffer.wrap(byteArrayOf(7))
            )
        }

        /*
         ┌───┬───┬───┬───┐
         │   │R/W│   │   │
         └───┴───┴───┴───┘
         */
        val readBytes = ByteArray(4)
        buf.get(readBytes)
        Assertions.assertArrayEquals(byteArrayOf(2, 3, 4, 5), readBytes)
        Assertions.assertEquals(0, buf.remainingCanGet())
        Assertions.assertEquals(4, buf.remainingCanPut())
    }

    /**
     * 测试 dst 为2个碎片, src 1个碎片
     */
    @Test
    fun testPutRingByteBuffer1() {
        val dst = RingBufferImpl(ByteBuffer.allocate(4))
        val src = RingBufferImpl(ByteBuffer.allocate(4))

        dst.put(byteArrayOf(1, 3, 5))
        dst.get(ByteArray(3)) //r=3,w=3,unread=0

        src.put(byteArrayOf(7, 8, 9)) //r=0,w=3,unread=3

        //测试前
        //src 有一个可读碎片: [0,2]
        //dsr 有两个可写碎片: [3,3],[0,2]
        dst.put(src)  //buf1: r=3,w=2,unread=2

        assertState(dst, 3, 2, 3, 1)
        assertState(src, 3, 3, 0, 4)
    }

    /**
     * 测试 dst 为1个碎片, src 2个碎片
     */
    @Test
    fun testPutRingByteBuffer2() {
        val src = RingBufferImpl(ByteBuffer.allocate(4))
        val dst = RingBufferImpl(ByteBuffer.allocate(4))

        src.put(byteArrayOf(1, 3, 5)) //src: r=0,w=3,unread=3
        src.get(ByteArray(3)) //src: r=3,w=3,unread=0
        src.put(byteArrayOf(1, 3, 5))  //src: r=3,w=2,unread=3

        //测试前
        //src 的两个可读碎片分别为:  3~3,0~1
        //dst 为一个可写碎片: 0~3

        dst.put(src)  //buf1: r=3,w=2,unread=2

        assertState(src, 2, 2, 0, 4)
        assertState(dst, 0, 3, 3, 1)
    }

    /**
     * 测试 dst 为2个碎片, src 2个碎片
     */
    @Test
    fun testPutRingByteBuffer3() {
        val src = RingBufferImpl(ByteBuffer.allocate(4))
        val dst = RingBufferImpl(ByteBuffer.allocate(4))

        src.put(byteArrayOf(1, 3, 5)) //buf1: r=0,w=3,unread=3
        src.get(ByteArray(3)) //buf1: r=3,w=3,unread=0
        src.put(byteArrayOf(2, 4, 6))  //buf1: r=3,w=2,unread=3

        dst.put(byteArrayOf(7, 8, 9)) //dst: r=0,w=3,unread=3
        dst.get(ByteArray(3)) //dst: r=3,w=3,unread=0

        //测试前
        //src 的两个可读碎片分别为:  [3~3],[0~1]
        //dst 为两个可写碎片: [3~3],[0~2]

        dst.put(src)

        assertState(src, 2, 2, 0, 4)
        assertState(dst, 3, 2, 3, 1)

        //检查内容是否正确
        val bytes = ByteArray(3)
        dst.get(bytes)
        Assertions.assertArrayEquals(byteArrayOf(2, 4, 6), bytes)

        //全部读出 应为空
        Assertions.assertEquals(0, dst.remainingCanGet())
    }

    private fun assertState(
        buf: RingBuffer,
        readPos: Int,
        writePos: Int,
        remainingCanGet: Int,
        remainingCanPut: Int
    ) {
        Assertions.assertArrayEquals(
            intArrayOf(readPos, writePos, remainingCanGet, remainingCanPut),
            intArrayOf(
                buf.readPosition(),
                buf.writePosition(),
                buf.remainingCanGet(),
                buf.remainingCanPut()
            )
        )
    }

}