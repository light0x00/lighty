package io.github.light0x00.letty.expr;

import io.github.light0x00.letty.expr.buffer.ReadWriteByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class ReadWriteBufferTest {

    @Test
    public void testRwBuf(){
        var rwBuffer = new ReadWriteByteBuffer(ByteBuffer.allocateDirect(4));

        ByteBuffer wBuffer = rwBuffer.bufferW();
        wBuffer.put((byte)1);
        wBuffer.put((byte)3);

        ByteBuffer rBuffer = rwBuffer.bufferR();

        Assertions.assertEquals( (byte) 1,rBuffer.get());
        Assertions.assertEquals( (byte) 3,rBuffer.get());
    }

}
