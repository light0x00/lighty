package io.github.light0x00.letty.core.buffer;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

public class DefaultBufferPool implements BufferPool {

    /**
     * 使用软引用,避免过多空闲的 buffer 导致 OOM , 作为兜底保障.
     * <p>
     * TODO 可引入 LRU/LFU 主动清理空闲的 buffer.
     */
    private final NavigableMap<Integer, SoftReference<Set<ByteBuffer>>> pool = new TreeMap<>();
    private final Function<Integer, ByteBuffer> bufferAllocator;

    public DefaultBufferPool(Function<Integer, ByteBuffer> byteBufferFactory) {
        this.bufferAllocator = byteBufferFactory;
    }

    private ByteBuffer takeOriginalBuffer(int capacityAtLeast) {
        synchronized (pool) {
            return pool.tailMap(capacityAtLeast)
                    .values()
                    .stream()
                    .map(SoftReference::get)
                    .filter(Objects::nonNull) //过滤掉被 GC 回收的
                    .map(DefaultBufferPool::removeNextOrNullInSet)
                    .filter(Objects::nonNull)
                    .findAny()
                    .map(buf -> {
                        buf.clear();  //清除之前的 buffer 状态
                        return buf;
                    })
                    .orElseGet(() -> bufferAllocator.apply(capacityAtLeast));
        }
    }

    @Override
    public RecyclableBuffer take(int capacity) {
        synchronized (pool) {
            ByteBuffer buf = takeOriginalBuffer(capacity);
            return new RecyclableBuffer(this, buf, 0, capacity);
        }
    }

    @Override
    public void recycle(RecyclableBuffer buffer) {
        recycle(buffer.backingBuffer);
    }

    private static <T> T removeNextOrNullInSet(Set<T> set) {
        Iterator<T> iterator = set.iterator();
        if (iterator.hasNext()) {
            T next = iterator.next();
            iterator.remove();
            return next;
        }
        return null;
    }

    private void recycle(ByteBuffer buffer) {
        synchronized (pool) {
            var ref = pool.get(buffer.capacity());
            //如果首次调用 get 对象存在,则应当附加一个强引用到该对象, 避免后续逻辑再次访问该对象时已被垃圾回收.
            var bufs = Optional.ofNullable(ref).map(SoftReference::get).orElse(null);

            if (ref == null || bufs == null) {
                bufs = new HashSet<>();
                bufs.add(buffer);
                pool.put(buffer.capacity(), new SoftReference<>(bufs));
            } else {
                bufs.add(buffer);
            }
        }
    }

}
