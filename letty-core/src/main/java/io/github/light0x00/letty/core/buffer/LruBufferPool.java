package io.github.light0x00.letty.core.buffer;

import io.github.light0x00.letty.core.util.LettyException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.GuardedBy;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author light0x00
 * @since 2023/7/29
 */
@Slf4j
public class LruBufferPool extends BufferPool {

    private final int maxBytes;

    private final ByteBufferAllocator bufferAllocator;

    @GuardedBy("poolLock")
    private final NavigableMap<Integer, Queue<ByteBuffer>> pool = new TreeMap<>();

    @GuardedBy("poolLock")
    private int bytesInPool;

    @GuardedBy("lruLock")
    private final LruQueue<Integer> lruQueue = new LruQueue<>();

    @GuardedBy("lruLock")
    private final Map<Integer, LruNode<Integer>> lruMap = new HashMap<>();

    /**
     * 为避免死锁, 加锁顺序必须为:
     *
     * <pre>{@code
     * synchronized(poolLock){ //1.
     *     synchronized(lruLock){ //2.
     *
     *     }
     * }
     * }</pre>
     */
    private final Object poolLock = new Object();
    private final Object lruLock = new Object();


    /**
     * @param maxBytes The maximum bytes of the ByteBuffers in buffer pool.
     *                 It will determine the threshold of the eviction.
     *                 When the size of the recycled ByteBuffers reach the threshold, an eviction will be triggered.
     */
    public LruBufferPool(ByteBufferAllocator bufferAllocator, int maxBytes) {
        this.bufferAllocator = bufferAllocator;
        this.maxBytes = maxBytes;
    }

    @Override
    public RecyclableBuffer take(int acquireCapacity) {
        ByteBuffer byteBuffer = takeAtLeast(acquireCapacity)
                .map(ByteBuffer::clear)
                .orElseGet(() -> {
                    log.debug("Allocate buffer: {} bytes", acquireCapacity);
                    return bufferAllocator.allocate(acquireCapacity);
                });
        return new RecyclableBuffer(this, byteBuffer, 0, acquireCapacity);
    }

    private Optional<ByteBuffer> takeAtLeast(int capacityAtLeast) {
        ByteBuffer chosen = null;
        synchronized (poolLock) {
            //1.从池子中查出大于 capacityAtLeast 的节点集合
            Collection<Queue<ByteBuffer>> queues = pool.tailMap(capacityAtLeast).values();
            Iterator<Queue<ByteBuffer>> queuesIterator = queues.iterator();

            if (queuesIterator.hasNext()) {
                //2.如果查出的集合非空, 即存在大于所申请容量的 buffer 队列, 则移出该队列
                Queue<ByteBuffer> candidateQueue = queuesIterator.next();
                //3.从移出的 buffer 队列中取出一个 buffer (作为返回结果)
                chosen = candidateQueue.poll();

                assert chosen != null;

                bytesInPool -= chosen.capacity();
                //4.如果移出后 buffer 队列没有剩余,则将队列移出池子
                if (candidateQueue.isEmpty()) {
                    //每当某种容量的 buffer 被取走, 导致所在队列为空, 此时意味这池子中以及不存在此中容量的 buffer, 应当从池子移除
                    queuesIterator.remove();
                    synchronized (lruLock) {
                        //5. lru 中维护的状态(queue 和 map)需要和池子保持一致, 当某个容量的 ByteBuffer 队列从池子移除, 则 lru 的数据结构也应当移除.
                        lruQueue.remove(lruMap.remove(chosen.capacity()));
                    }
                } else {
                    //4.每当某种容量的 buffer 被取走, 且池子中该容量的 buffer 还有剩余时
                    //都将该容量从 lru queue 中移动到末尾(表示 “该容量的 buffer 最近访问过”)
                    synchronized (lruLock) {
                        log.debug("update lru order:{}", chosen.capacity());
                        lruQueue.moveToTail(lruMap.get(chosen.capacity()));
                    }
                }
            }
        }
        return Optional.ofNullable(chosen);
    }

    @Override
    protected void recycle(RecyclableBuffer recyclable) {
        if (!recyclable.hasReleased && !recyclable.markReleased()) {
            return;
        }
        ByteBuffer buf = recyclable.backingBuffer;

        int capacity = buf.capacity();
        synchronized (poolLock) {
            synchronized (lruLock) {
                //当剩余空间不足时,触发淘汰
                //keep the remaining space in pool greater than the specified capacity.
                makeSpace(capacity);
                if (!lruMap.containsKey(capacity)) {
                    lruMap.put(capacity, lruQueue.enqueue(capacity));
                }
            }
            pool.computeIfAbsent(buf.capacity(), key -> new LinkedList<>())
                    .add(buf);
            bytesInPool += capacity;
        }
    }

    /**
     * keep the remaining space in pool greater than the specified capacity.
     * 基于 lru 队列，循环淘汰最不常用的容量的 buffer, 知道池子剩余空间足够为止(或者抛出异常,当全部淘汰池子中所有的 buffer 仍不足时)
     *
     * @implNote Must be invoked in critical section guarded by {@link #lruLock} and {@link #poolLock}
     */
    private void makeSpace(int capacity) {
        while (maxBytes - bytesInPool < capacity) {
            if (lruQueue.isEmpty()) {
                //这种情况, 意味着池子为空, 且要申请的 buffer 容量大于池子的最大容量
                throw new LettyException("Buffer pool space not enough! The max bytes of pool is {}, but the required is {}", maxBytes, capacity);
            }
            Integer capacityToEvict = lruQueue.peek();

            Queue<ByteBuffer> candidatesToEvict = pool.get(capacityToEvict);
            while (!candidatesToEvict.isEmpty()) {
                bytesInPool -= candidatesToEvict.poll().capacity();
                log.debug("evict {}", capacityToEvict);
                if (maxBytes - bytesInPool >= capacity) {
                    break;
                }
            }
            if (candidatesToEvict.isEmpty()) {
                pool.remove(capacityToEvict); //if the queue is empty, dereference it
                lruQueue.remove(lruMap.remove(capacityToEvict));
            }
            //When go here, neither the candidate queue is empty or bytesInPool <= maxBytes ,
            //the former the outer loop continue, the later the work finished.
        }
    }

    private static class LruNode<T> {
        T value;
        LruNode<T> prev;
        LruNode<T> next;

        public LruNode() {
        }

        public LruNode(T value) {
            this.value = value;
        }
    }

    private static class LruQueue<T> {
        LruNode<T> virtualHead = new LruNode<>();
        LruNode<T> virtualTail = new LruNode<>();
        int size;

        public LruQueue() {
            virtualHead.next = virtualTail;
            virtualTail.prev = virtualHead;
        }

        LruNode<T> enqueue(T t) {
            LruNode<T> cur = new LruNode<>(t);
            LruNode<T> prev = virtualTail.prev;

            prev.next = cur;
            cur.prev = prev;

            cur.next = virtualTail;
            virtualTail.prev = cur;
            size += 1;
            return cur;
        }

        LruNode<T> dequeue() {
            if (size == 0) {
                return null;
            }
            LruNode<T> cur = virtualHead.next;
            detach(cur);
            size -= 1;
            return cur;
        }

        T peek() {
            if (size >= 0) {
                return virtualHead.next.value;
            }
            return null;
        }

        void remove(LruNode<T> node) {
            detach(node);
            size -= 1;
        }

        void detach(LruNode<T> node) {
            LruNode<T> prev = node.prev;
            LruNode<T> next = node.next;

            prev.next = next;
            next.prev = prev;
        }

        void moveToTail(LruNode<T> node) {
            if (size < 2) {
                return;
            }
            detach(node);

            LruNode<T> oldTail = virtualTail.prev;
            oldTail.next = node;
            node.prev = oldTail;

            node.next = virtualTail;
            virtualTail.prev = node;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
