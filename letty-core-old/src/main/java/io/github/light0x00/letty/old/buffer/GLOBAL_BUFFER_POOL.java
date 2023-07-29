package io.github.light0x00.letty.old.buffer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;

public class GLOBAL_BUFFER_POOL {
    private static BufferPool<ByteBuffer> value;
    private static final VarHandle VALUE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findStaticVarHandle(GLOBAL_BUFFER_POOL.class, "value", BufferPool.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected static void init(BufferPool<ByteBuffer> bufferPool) {
        VALUE.compareAndSet(null, bufferPool);
    }

    public static void recycle(ByteBuffer buf) {
        value.recycle(buf);
    }

    public static RecyclableByteBuffer<ByteBuffer> take(int capacity) {
        return value.take(capacity);
    }
}
