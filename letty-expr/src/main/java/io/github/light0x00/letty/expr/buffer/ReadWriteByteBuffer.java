package io.github.light0x00.letty.expr.buffer;

import java.nio.ByteBuffer;

@Deprecated
public class ReadWriteByteBuffer{

    private final ByteBuffer bufferR;
    private final ByteBuffer bufferW;

    @SuppressWarnings("unchecked")
    public ReadWriteByteBuffer(ByteBuffer originalBuffer) {
        this.bufferW = originalBuffer;
        this.bufferR = bufferW.asReadOnlyBuffer();
    }

    public ByteBuffer bufferR() {
        beforeAccessBuffer();
        return bufferR;
    }

    public ByteBuffer bufferW() {
        beforeAccessBuffer();
        return bufferW;
    }


    public int capacity(){
        beforeAccessBuffer();
        return bufferR.capacity();
    }

    protected void beforeAccessBuffer(){
    }

}
