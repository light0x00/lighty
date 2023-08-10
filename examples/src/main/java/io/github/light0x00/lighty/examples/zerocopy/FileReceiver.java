package io.github.light0x00.lighty.examples.zerocopy;

import io.github.light0x00.lighty.core.buffer.RecyclableBuffer;
import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * @author light0x00
 * @since 2023/8/7
 */
@Slf4j
class FileReceiver extends InboundChannelHandlerAdapter {

    final FileChannel fileChannel;

    final Path filepath;

    long timeBegin;

    {
        try {
            filepath = Paths.get(System.getProperty("user.home"), "zero-copy-download-test-" + UUID.randomUUID());
            fileChannel = FileChannel.open(filepath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onConnected(ChannelContext context) {
        timeBegin = System.currentTimeMillis();
    }

    @SneakyThrows
    @Override
    public void onRead(ChannelContext context, Object data, InboundPipeline next) {
        try (RecyclableBuffer buffer = (RecyclableBuffer) data) {
            while (buffer.remainingCanGet() > 0) {
                int n = buffer.writeToChannel(fileChannel);
                log.info("Received: {} bytes", n);
            }
        }
    }

    @SneakyThrows
    @Override
    public void onReadCompleted(ChannelContext context) {
        fileChannel.close();
        log.info("File saved!, time elapsed: {} ms", System.currentTimeMillis() - timeBegin);
    }
}
