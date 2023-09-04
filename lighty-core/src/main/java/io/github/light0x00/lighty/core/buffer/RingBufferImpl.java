package io.github.light0x00.lighty.core.buffer;

import io.github.light0x00.lighty.core.util.Tool;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author light0x00
 * @since 2023/7/5
 */
@NotThreadSafe
public class RingBufferImpl implements RingBuffer {

    private final ByteBuffer buffer;

    @Getter
    private int writePosition;

    @Getter
    private int readPosition;

    private int bytesUnRead;

    @Getter
    final int capacity;

    /**
     * @param buffer        the baking buffer
     * @param readPosition  the read position
     * @param writePosition the write position
     * @param capacity      the size of the ring buffer, start with 0.
     * @param allUnRead     when the read and write position overlap, indicate that weather the ring buffer is full or empty.
     */
    public RingBufferImpl(ByteBuffer buffer, int readPosition, int writePosition, int capacity, boolean allUnRead) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        if (readPosition > capacity || readPosition < 0) {
            throw new BufferUnderflowException();
        }
        if (writePosition > capacity || writePosition < 0) {
            throw new BufferOverflowException();
        }

        this.buffer = buffer;
        this.capacity = capacity;
        this.readPosition = readPosition;
        this.writePosition = writePosition;

        if (readPosition == writePosition) { //读写指针相等, 意味着要么全部未读, 要么全部已读.
            bytesUnRead = allUnRead ? capacity : 0; //这里根据标识决定属于哪种情况.
        } else if (readPosition < writePosition) {
            bytesUnRead = writePosition - readPosition;
        } else {
            bytesUnRead = (writePosition) + (capacity - readPosition);
        }
    }


    public RingBufferImpl(ByteBuffer buffer) {
        this(buffer, 0, 0, buffer.capacity(), false);
    }

    /**
     * @see ByteBuffer#get()
     */
    @Override
    public byte get() {
        if (bytesUnRead <= 0) {
            throw new BufferUnderflowException();
        }
        byte b = buffer.get(readPosition);
        moveReadPosition(1);
        return b;
    }

    @Override
    public RingBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * @see ByteBuffer#get(byte[], int, int)
     */
    @Override
    public RingBuffer get(byte[] dst, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, dst.length);

        if (length > bytesUnRead) { //要读取的字节多余剩余可读字节
            throw new BufferUnderflowException();
        }

        ByteBuffer[] slices = readableSlices();
        int hasRead = 0; //读出数量
        for (ByteBuffer slice : slices) {
            int n = Math.min(slice.remaining(), length - hasRead);
            slice.get(dst, offset + hasRead, n);
            if (hasRead < length) {
                hasRead += n;
            } else {
                break;
            }
        }
        moveReadPosition(length);
        return this;
    }

    /**
     * @see ByteBuffer#put(byte)
     */
    @Override
    public RingBuffer put(final byte value) {
        if (bytesUnRead >= capacity) {
            throw new BufferOverflowException();
        }
        buffer.put(writePosition, value);
        moveWritePosition(1);
        return this;
    }

    @Override
    public RingBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    /**
     * @see ByteBuffer#put(byte[], int, int)
     */
    @Override
    public RingBuffer put(byte[] src, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, src.length);

        if (length > remainingCanPut()) {
            throw new BufferOverflowException();
        }
        int hasWrite = 0;
        ByteBuffer[] slices = writableSlices();
        for (ByteBuffer slice : slices) {
            int n = Math.min(slice.remaining(), length - hasWrite);
            slice.put(src, offset + hasWrite, n);
            if (hasWrite < length) {
                hasWrite += n;
            } else {
                break;
            }
        }
        moveWritePosition(length);
        return this;
    }

    /**
     * @see ByteBuffer#put(ByteBuffer)
     */
    @Override
    public RingBuffer put(ByteBuffer src) {
        put(src, src.position(), src.remaining());
        src.position(src.position() + src.remaining());
        return this;
    }

    /**
     * @implNote The positions of the src buffer are then incremented by length.
     */
    @Override
    public RingBuffer put(ByteBuffer src, int length) {
        put(src, src.position(), length);
        src.position(src.position() + src.remaining());
        return this;
    }

    /**
     * @throws IndexOutOfBoundsException If the preconditions on the {@code index}, {@code offset}, and
     *                                   {@code length} parameters do not hold
     * @throws ReadOnlyBufferException   If this buffer is read-only
     * @implNote The positions of the src buffer are unchanged.
     */
    @Override
    public RingBuffer put(ByteBuffer src, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, src.limit());
        if (length > remainingCanPut()) { //要放入的字节多余剩余空间
            throw new BufferOverflowException();
        }
        if (writePosition >= readPosition) {
            int written = Math.min(capacity - writePosition, length);
            buffer.put(writePosition, src, offset, written);
            if (written < length) //右边空间不够, 则往左边写
                buffer.put(0, src, offset + written, length - written);
        } else {
            buffer.put(writePosition, src, offset, length);
        }
        moveWritePosition(length);
        return this;
    }

    @Override
    public RingBuffer put(RingBuffer src) {
        return put(src, src.remainingCanGet());
    }

    /**
     * The read positions of the src buffer are then incremented by length.
     * The write positions of this buffer are then incremented by length.
     */
    @Override
    public RingBuffer put(RingBuffer src, int length) {
        if (length <= 0) {
            throw new IndexOutOfBoundsException();
        }
        if (length > src.remainingCanGet()) {
            throw new BufferUnderflowException();
        }
        if (length > remainingCanPut()) {
            throw new BufferOverflowException();
        }

        ByteBuffer[] srcSlices = src.readableSlices();
        ByteBuffer[] dstSlices = writableSlices();

        int sIdx = 0;
        int dIdx = 0;

        int putCnt = 0;
        while (sIdx < srcSlices.length && dIdx < dstSlices.length) {
            ByteBuffer s = srcSlices[sIdx];
            ByteBuffer d = dstSlices[dIdx];

            int sRemaining = s.remaining(); //源碎片 可读数量
            int dRemaining = d.remaining(); //目的碎片 可写数量

            int putLen = Math.min(Math.min(sRemaining, dRemaining), length - putCnt);
            d.put(0, s, 0, putLen);

            if ((putCnt += putLen) == length) {
                break;
            }

            sRemaining -= putLen;
            dRemaining -= putLen;

            if (sRemaining == 0) {
                sIdx++;
            }
            if (dRemaining == 0) {
                dIdx++;
            }
        }
        moveWritePosition(length);
        src.moveReadPosition(length);
        return this;
    }

    /**
     * Read data from channel to this buffer.
     */
    @Override
    public int readFromChannel(ScatteringByteChannel channel) throws IOException {
        ByteBuffer[] slices = writableSlices();
        if (slices.length > 0) {
            //Impossible to overflow the range of int,
            //cuz to the buffers are fragments from only one ByteBuffer, which is at most Integer.MAX_VALUE bytes
            int n = (int) channel.read(slices);
            if (n > 0)
                moveWritePosition(n);
            return n;
        }
        return 0;
    }

    /**
     * Write data from buffer to channel.
     */
    @Override
    public int writeToChannel(GatheringByteChannel channel) throws IOException {
        ByteBuffer[] slices = readableSlices();
        if (slices.length > 0) {
            int n = (int) channel.write(slices); //不可能超过 int 范围
            if (n > 0)
                moveReadPosition(n);
            return n;
        }
        return 0;
    }

    @Override
    public int getInt() {
        byte[] bytes = new byte[4];
        if (remainingCanGet() < 4) {
            throw new BufferUnderflowException();
        }
        get(bytes);
        return Tool.bytesToInt(bytes);
    }

    @Override
    public RingBuffer putInt(int value) {
        if (remainingCanPut() < 4) {
            throw new BufferOverflowException();
        }
        return put(Tool.intToBytes(value));
    }

    @Override
    public long getLong() {
        byte[] bytes = new byte[8];
        if (remainingCanGet() < 8) {
            throw new BufferUnderflowException();
        }
        get(bytes);
        return Tool.bytesToLong(bytes);
    }

    @Override
    public RingBuffer putLong(long value) {
        if (remainingCanPut() < 8) {
            throw new BufferOverflowException();
        }
        return put(Tool.longToBytes(value));
    }

    /**
     * Decode as String.
     */
    @Override
    public String getString(Charset charset) {
        ByteBuffer[] slices = readableSlices();
        if (slices.length == 0) {
            return "";
        }
        byte[] array;
        if (slices.length > 1) {
            array = new byte[remainingCanGet()];
            get(array);
        } else {
            array = slices[0].array();
        }
        return new String(array, charset);
    }

    @Override
    public int remainingCanGet() {
        return bytesUnRead;
    }

    @Override
    public int remainingCanPut() {
        return capacity - bytesUnRead;
    }

    @Override
    public void clear() {
        readPosition = 0;
        writePosition = 0;
        bytesUnRead = 0;
    }

    @Override
    public ByteBuffer[] readableSlices() {
        return Arrays.stream(readableFragments().toArray())
                .map(Fragment::sliceBuf)
                .toArray(ByteBuffer[]::new);
    }

    public ByteBuffer[] writableSlices() {
        return Arrays.stream(writableFragments().toArray())
                .map(Fragment::sliceBuf)
                .toArray(ByteBuffer[]::new);
    }

    private FragmentList readableFragments() {
        FragmentList fragments = new FragmentList();
        /*
            * r * w
            0 1 2 3
         */
        if (writePosition > readPosition) {
            fragments.offer(new Fragment(readPosition, writePosition - readPosition));
        }
        /*
            * w * r
            0 1 2 3
        */
        else if (writePosition < readPosition) {
            fragments.offer(new Fragment(readPosition, capacity - readPosition));
            if (writePosition > 0) {
                fragments.offer(new Fragment(0, writePosition));
            }
        }
        /*
            * w/r * *
            0  1  2 3
        */
        else {
            if (bytesUnRead == capacity) { // 写指针追上读指针, 则存在可读数据, 反之则没有可读数据
                fragments.offer(new Fragment(writePosition, capacity - writePosition));
                if (writePosition > 0) {
                    fragments.offer(new Fragment(0, writePosition));
                }
            }
        }
        return fragments;
    }

    private FragmentList writableFragments() {

        FragmentList fragments = new FragmentList();
        /*
            * r * w
            0 1 2 3
         */
        if (writePosition > readPosition) {
            fragments.offer(new Fragment(writePosition, capacity - writePosition));
            if (readPosition > 0) {
                fragments.offer(new Fragment(0, readPosition));
            }
        }
        /*
            * w * r
            0 1 2 3
        */
        else if (writePosition < readPosition) {
            fragments.offer(new Fragment(writePosition, readPosition - writePosition));
        }
        /*
            * w/r * *
            0  1  2 3
        */
        else {
            if (bytesUnRead == 0) {   // 读指针追上写指针, 则存在可写空间 , 反之则没有剩余可写空间
                fragments.offer(new Fragment(writePosition, capacity - writePosition));
                if (writePosition > 0) {
                    fragments.offer(new Fragment(0, writePosition));
                }
            }
        }
        return fragments;
    }

    private int nextPosition(int position) {
        int next = position + 1;
        if (next == capacity) {
            return 0;
        }
        return next;
    }

    @Override
    public void moveReadPosition(int step) {
        if (remainingCanGet() < step) {
            throw new BufferUnderflowException();
        }
        readPosition = (readPosition + step) % capacity;
        bytesUnRead -= step;
    }

    @Override
    public void moveWritePosition(int step) {
        if (remainingCanPut() < step) {
            throw new BufferOverflowException();
        }
        writePosition = (writePosition + step) % capacity;
        bytesUnRead += step;
    }

    /**
     * FIFO queue
     */
    private class FragmentList implements Iterable<Fragment> {
        @Getter
        private int size;
        private final Fragment HEAD = new Fragment();
        private Fragment cur = HEAD;

        public void offer(Fragment fragment) {
            cur.next = fragment;
            cur = fragment;
            size++;
        }

        @Nullable
        public Fragment poll() {
            var p = HEAD.next;
            if (p != null) {
                HEAD.next = p.next;
            }
            return p;
        }

        @Nullable
        public Fragment first() {
            return HEAD.next;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public Fragment[] toArray() {
            Fragment[] arr = new Fragment[size];
            int i = 0;
            for (Fragment fragment : this) {
                arr[i++] = fragment;
            }
            return arr;
        }

        @Nonnull
        @Override
        public Iterator<Fragment> iterator() {
            return new Iterator<>() {
                Fragment cur = HEAD;

                @Override
                public boolean hasNext() {
                    return cur.next != null;
                }

                @Override
                public Fragment next() {
                    cur = cur.next;
                    return cur;
                }
            };
        }
    }

    private class Fragment {

        @Getter
        private int offset;

        @Getter
        private int length;

        RingBuffer ring;

        Fragment next;

        Fragment() {
            //only for pseudo node usage
        }

        Fragment(int offset, int length) {
            this.offset = offset;
            this.length = length;
            this.ring = RingBufferImpl.this;
        }

        /**
         * 碎片对应的 buffer 切片
         */
        ByteBuffer sliceBuf() {
            return buffer.slice(offset, length);
        }
    }

}
