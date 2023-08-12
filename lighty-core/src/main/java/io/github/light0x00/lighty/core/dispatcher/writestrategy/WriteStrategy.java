package io.github.light0x00.lighty.core.dispatcher.writestrategy;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;

/**
 * @author light0x00
 * @since 2023/8/12
 */
public interface WriteStrategy {
    long write(GatheringByteChannel channel) throws IOException;

    long remaining();

    Object getSource();
}
