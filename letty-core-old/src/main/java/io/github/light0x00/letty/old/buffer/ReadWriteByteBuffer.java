package io.github.light0x00.letty.old.buffer;

import java.nio.ByteBuffer;

public class ReadWriteByteBuffer<T extends ByteBuffer> {

    private final T readSideBuffer;
    private final T writeSideBuffer;

    @SuppressWarnings("unchecked")
    public ReadWriteByteBuffer(T originalBuffer) {
        this.writeSideBuffer = originalBuffer;
        this.readSideBuffer = (T) writeSideBuffer.asReadOnlyBuffer();
    }

    public T readUponBuffer() {
        beforeAccessBuffer();
        return readSideBuffer;
    }

    public T writeUponBuffer() {
        beforeAccessBuffer();
        return writeSideBuffer;
    }

    public int capacity(){
        beforeAccessBuffer();
        return readSideBuffer.capacity();
    }

    protected void beforeAccessBuffer(){
    }
}
