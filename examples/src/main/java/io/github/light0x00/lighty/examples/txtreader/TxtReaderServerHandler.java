package io.github.light0x00.lighty.examples.txtreader;

import io.github.light0x00.lighty.core.handler.ChannelContext;
import io.github.light0x00.lighty.core.handler.InboundChannelHandlerAdapter;
import io.github.light0x00.lighty.core.handler.InboundPipeline;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author light0x00
 * @since 2023/8/21
 */
@Slf4j
public class TxtReaderServerHandler extends InboundChannelHandlerAdapter {

    static final Map<Integer, String> titles = new LinkedHashMap<>();

    static final Map<Integer, Path> contents = new LinkedHashMap<>();

    static {
        loadResources();
    }

    @Override
    public void onConnected(@Nonnull ChannelContext context) {
        sendMenu(context);
    }

    @SneakyThrows
    @Override
    public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) {
        sendSplitLine(context);
        String command = String.valueOf(data);
        if ("".equals(command)) {
            return;
        }
        try {
            int num = Integer.parseInt(command);
            sendContent(context, num);
        } catch (NumberFormatException e) {
            processCommand(context, command);
        }
    }

    private void sendMenu(@Nonnull ChannelContext context) {
        sendSplitLine(context);

        String list = titles.entrySet().stream().map(e -> e.getKey() + " 《" + e.getValue() + "》")
                .collect(Collectors.joining("\n"));

        String menu = """

                🌷 Welcome to visit poetry server! 🌷

                %s

                Choose the one you'd love to read (input `quit` to exit):"""
                .formatted(list);

        context.writeAndFlush(menu);
    }

    private void sendContent(@Nonnull ChannelContext context, int num) throws IOException {
        Path path = contents.get(num);

        if (path == null) {
            context.writeAndFlush("Not a valid number!");
        } else {
            context.transfer(FileChannel.open(path, StandardOpenOption.READ));
            sendSplitLine(context);
            context.writeAndFlush("Input `h` back to home page:");
        }
    }

    private void processCommand(@Nonnull ChannelContext context, String command) {
        if ("q".equals(command)) {
            context.writeAndFlush("Bye!")
                    .addListener(future -> context.close());
        } else if ("h".equals(command)) {
            sendMenu(context);
        } else {
            context.writeAndFlush("Unrecognized command:" + command);
        }
    }

    private void sendSplitLine(@Nonnull ChannelContext context) {
        context.write("\n\n----------------------------------------------------------------------------\n\n");
    }

    private static void loadResources() {
        File root = new File(Objects.requireNonNull(TxtReaderServerHandler.class.getClassLoader().getResource("txt"))
                .getFile());

        File[] items = Objects.requireNonNull(root.listFiles((dir, name) -> name.endsWith(".txt")));

        int i = 0;
        for (File item : items) {
            titles.put(++i, item.getName().replaceAll("\\.txt$", ""));
            contents.put(i, Paths.get(item.getPath()));
        }
    }
}
