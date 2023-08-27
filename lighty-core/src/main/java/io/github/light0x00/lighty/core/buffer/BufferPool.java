package io.github.light0x00.lighty.core.buffer;

/**
 * @author light0x00
 * @since 2023/7/27
 */
public abstract class BufferPool {

    public abstract ByteBuf take(int capacity);

    protected abstract void recycle(ByteBuf buffer);
}
