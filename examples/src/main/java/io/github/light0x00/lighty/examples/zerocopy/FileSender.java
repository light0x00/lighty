package io.github.light0x00.lighty.examples.zerocopy;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.ChannelHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
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
    public void onConnected(@Nonnull ChannelContext context) {
        long timeBegin = System.currentTimeMillis();
        context.transfer(FileChannel.open(filePath, StandardOpenOption.READ))
                .addListener(future -> {
                    log.info("File send result: {}", future.isSuccess());
                    if (future.isSuccess()) {
                        log.info("File sending completed! time elapsed: {}ms", System.currentTimeMillis() - timeBegin);
                        context.channel().close();
                    } else {
                        future.cause().printStackTrace();
                    }
                });
        context.flush();
    }
}
