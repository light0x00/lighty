package io.github.light0x00.lighty.core.dispatcher;

import io.github.light0x00.lighty.core.util.EventLoopConfinement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author light0x00
 * @since 2023/8/12
 */
@EventLoopConfinement
@Slf4j
public class OutputBuffer {
    private final Queue<WriterFuturePair> outputBuffer = new LinkedList<>();

    @Getter
    private int size;

    public void offer(WriterFuturePair wf) {
        outputBuffer.offer(wf);
        size += wf.writer().remaining();
    }

    public WriterFuturePair poll() {
        var bf = outputBuffer.poll();
        if (bf != null)
            size -= bf.writer().remaining();
        return bf;
    }

    public WriterFuturePair peek() {
        return outputBuffer.peek();
    }

    public boolean isEmpty() {
        return outputBuffer.isEmpty();
    }

    public void invalid(Throwable cause) {
        for (WriterFuturePair bufFuture; (bufFuture = outputBuffer.poll()) != null; ) {
            bufFuture.future().setFailure(cause);
        }
        if (size > 0)
            log.debug("Invalid output buffer {} bytes, due to forced close", size);
    }
}
