package io.github.light0x00.lighty.core;

public interface LightyProperties {

    /**
     * Returns {@code true} if and only if the channel should not close itself when its remote
     * peer shuts down output to make the connection half-closed.  If {@code false}, the connection
     * is closed automatically when the remote peer shuts down output.
     */
    boolean isAllowHalfClosure();

    /**
     * When a channel is readable, the bytes to read per times.
     * It will determine the size of the ByteBuffer to load the incoming bytes in Channel.
     */
    int readBufSize();

    /**
     * The maximum bytes of the ByteBuffers in buffer pool.
     * It will determine the threshold of the eviction.
     * When the size of the recycled ByteBuffers reach the threshold, an eviction will be triggered.
     */
    int bufferPoolMaxSize();

}
