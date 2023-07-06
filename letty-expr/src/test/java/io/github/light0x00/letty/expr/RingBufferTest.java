package io.github.light0x00.letty.expr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static io.github.light0x00.letty.expr.toolkit.Tool.bytesToInt;
import static io.github.light0x00.letty.expr.toolkit.Tool.intToBytes;

/**
 * @author light0x00
 * @since 2023/7/5
 */
public class RingBufferTest {

    @Test
    public void testGetWhatPut() {
        RingByteBuffer buf = new RingByteBuffer(ByteBuffer.allocate(4));

        buf.put((byte) 1);
        buf.put((byte) 3);
        buf.put((byte) 5);

        Assertions.assertEquals((byte) 1, buf.get());
        Assertions.assertEquals((byte) 3, buf.get());
        Assertions.assertEquals((byte) 5, buf.get());
    }

    @Test
    public void testGetWhatPut2() {
        RingByteBuffer buf = new RingByteBuffer(ByteBuffer.allocate(4));

        int cp = "😅".codePoints().findFirst().getAsInt();

        System.out.println(Character.toString(cp));

        buf.put(intToBytes(cp));

        byte[] cpb = new byte[4];
        buf.get(cpb);

        Assertions.assertEquals(
                "😅",
                Character.toString(bytesToInt(cpb))
        );
    }

    @Test
    public void testPutOverflow() {
        RingByteBuffer buf = new RingByteBuffer(ByteBuffer.allocate(4));
        Assertions.assertThrows(BufferOverflowException.class, () -> buf.put(new byte[]{0, 1, 2, 3, 4}));
    }

    @Test
    public void testGetUnderflow() {
        RingByteBuffer buf = new RingByteBuffer(ByteBuffer.allocate(4));
        buf.put(new byte[]{1, 2});
        Assertions.assertThrows(BufferUnderflowException.class, () -> buf.get(new byte[3]));
    }

    /**
     * 轮流执行 put 、 get
     */
    @Test
    public void testPutAndGetAlternate() {
        RingByteBuffer buf = new RingByteBuffer(ByteBuffer.allocate(4));
        /*
         ┌───┬───┬───┬───┐
         │ R │   │ W │   │
         └───┴───┴───┴───┘
         */
        buf.put(new byte[]{1, 2});

        /*
         ┌───┬───┬───┬───┐
         │   │ R │ W │   │
         └───┴───┴───┴───┘
         */
        Assertions.assertEquals((byte) 1, buf.get());
        Assertions.assertEquals(1, buf.remainingCanGet());
        Assertions.assertEquals(3, buf.remainingCanPut());

        Assertions.assertThrows(BufferOverflowException.class, () -> buf.put(new byte[]{3, 4, 5, 6}));

        /*
         ┌───┬───┬───┬───┐
         │   │W/R│   │   │
         └───┴───┴───┴───┘
         */
        buf.put(new byte[]{3, 4, 5}, 0, 3);
        Assertions.assertEquals(4, buf.remainingCanGet());
        Assertions.assertEquals(0, buf.remainingCanPut());

        Assertions.assertThrows(BufferOverflowException.class, () -> buf.put((byte) 7));


        /*
         ┌───┬───┬───┬───┐
         │   │R/W│   │   │
         └───┴───┴───┴───┘
         */
        byte[] readBytes = new byte[4];
        buf.get(readBytes);
        Assertions.assertArrayEquals(new byte[]{2, 3, 4, 5}, readBytes);

        Assertions.assertEquals(0, buf.remainingCanGet());
        Assertions.assertEquals(4, buf.remainingCanPut());
    }

    @Test
    public void testPutAndGetAlternate2() {
        RingByteBuffer buf = new RingByteBuffer(ByteBuffer.allocate(4));
        /*
         ┌───┬───┬───┬───┐
         │ R │   │ W │   │
         └───┴───┴───┴───┘
         */
        buf.put(ByteBuffer.wrap(new byte[]{1, 2}));
        Assertions.assertEquals(2, buf.remainingCanGet());
        Assertions.assertEquals(2, buf.remainingCanPut());

        /*
         ┌───┬───┬───┬───┐
         │   │ R │ W │   │
         └───┴───┴───┴───┘
         */
        Assertions.assertEquals((byte) 1, buf.get());
        Assertions.assertEquals(1, buf.remainingCanGet());
        Assertions.assertEquals(3, buf.remainingCanPut());

        Assertions.assertThrows(BufferOverflowException.class, () -> buf.put(ByteBuffer.wrap(new byte[]{3, 4, 5, 6})));

        /*
         ┌───┬───┬───┬───┐
         │   │W/R│   │   │
         └───┴───┴───┴───┘
         */
        buf.put(new byte[]{3, 4, 5, 6}, 0, 3);
        Assertions.assertEquals(4, buf.remainingCanGet());
        Assertions.assertEquals(0, buf.remainingCanPut());

        Assertions.assertThrows(BufferOverflowException.class, () -> buf.put(ByteBuffer.wrap(new byte[]{7})));


        /*
         ┌───┬───┬───┬───┐
         │   │R/W│   │   │
         └───┴───┴───┴───┘
         */
        byte[] readBytes = new byte[4];
        buf.get(readBytes);
        Assertions.assertArrayEquals(new byte[]{2, 3, 4, 5}, readBytes);

        Assertions.assertEquals(0, buf.remainingCanGet());
        Assertions.assertEquals(4, buf.remainingCanPut());
    }


}

