package io.github.light0x00.letty.core.buffer;

/**
 * @author light0x00
 * @since 2023/7/27
 */
public interface BufferPool {

    RecyclableBuffer take(int capacity);

    void recycle(RecyclableBuffer buffer);
}
