package io.github.light0x00.lighty.core.dispatcher.writestrategy;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;

/**
 * @author light0x00
 * @since 2023/8/12
 */
@AllArgsConstructor
@Slf4j
public class FileChannelWriteStrategy implements WriteStrategy {
    private FileChannel fileChannel;
    private long position;

    public FileChannelWriteStrategy(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    @Override
    public long write(GatheringByteChannel channel) throws IOException {
        long written = fileChannel.transferTo(position, fileChannel.size() - position, channel);
        position += written;
        return written;
    }

    @SneakyThrows
    @Override
    public long remaining() {
        return fileChannel.size() - position;
    }

    @Override
    public Object getSource() {
        return fileChannel;
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
