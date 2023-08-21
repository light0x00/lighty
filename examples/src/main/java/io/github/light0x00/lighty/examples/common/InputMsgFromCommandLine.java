package io.github.light0x00.lighty.examples.common;

import io.github.light0x00.lighty.core.facade.NioSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

/**
 * @author light0x00
 * @since 2023/8/2
 */
@Slf4j
public class InputMsgFromCommandLine implements Runnable {
    NioSocketChannel channel;

    public InputMsgFromCommandLine(NioSocketChannel channel) {
        this.channel = channel;
    }

    @Getter
    volatile Thread runner;

    @Override
    public void run() {
        runner = Thread.currentThread();

        channel.closedFuture().addListener(f -> runner.interrupt());

        while (!Thread.currentThread().isInterrupted()) {
            var scanner = new Scanner(System.in);
            String i = scanner.nextLine();
            if ("q".equals(i)) {
                channel.close();
                break;
            }
            channel.writeAndFlush(i).addListener(future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                    runner.interrupt();
                }
            });
        }
    }

}
