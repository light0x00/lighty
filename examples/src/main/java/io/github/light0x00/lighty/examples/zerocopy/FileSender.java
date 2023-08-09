package io.github.light0x00.lighty.examples.zerocopy;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.adapter.ChannelHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author light0x00
 * @since 2023/8/7
 */
@Slf4j
class FileSender extends ChannelHandlerAdapter {
    private final Path filePath;

    public FileSender(Path filePath) {
        this.filePath = filePath;
    }

    @SneakyThrows
    @Override
    public void onConnected(ChannelContext context) {
        long timeBegin = System.currentTimeMillis();
        context.channel()
                .write(FileChannel.open(filePath, StandardOpenOption.READ))
                .addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("File sending completed! time elapsed: {}ms", System.currentTimeMillis() - timeBegin);
                        context.channel().close();
                    } else {
                        future.cause().printStackTrace();
                    }
                });
    }
}