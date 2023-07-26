package io.github.light0x00.letty.core;

public interface LettyConfig {

    /**
     * Returns {@code true} if and only if the channel should not close itself when its remote
     * peer shuts down output to make the connection half-closed.  If {@code false}, the connection
     * is closed automatically when the remote peer shuts down output.
     */
    boolean isAllowHalfClosure();

    default int readBufSize(){
        return 16;
    }

}
