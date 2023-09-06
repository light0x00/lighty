[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.light0x00/lighty/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.light0x00/lighty-all)
[![Java support](https://img.shields.io/badge/Java-17+-green?logo=java&logoColor=white)](https://openjdk.java.net/)

<p align="center">
    <img src="doc/logo.png" alt="Lighty">
</p>

[中文](./README.md) | English

## 📖Introduction

Lighty is an elegant, event-driven, asynchronous network framework. The pursuit of **elegance** in the **internal structure** and **user interface** is the first essence of Lighty's design. With Lighty, you'll feel its minimalism, logical consistency, sense of boundaries, and spirituality, rather than the disquieting mechanical feel that pervades many popular technologies.

Its original design inspiration comes from Doug Lea's ***Scalable I/O in Java***, and the theoretical support for concurrency control is also basically from Doug Lea's drafting of JSR133, JEP188 related documents. Beside that, Lighty was inspired by Netty, and there is no doubt that many of Netty's designs are optimal solutions to network framework.

Lighty is implemented based on the Multiple Reactors pattern and has the following features:

- **100% lock-less**: Lighty's I/O processing is completely lock-free throughout the entire process, thanks to techniques such as thread stack isolation, inter-components asynchronous communication, and thread binding.
- **Pipeline**: Based on the object behavioral pattern, Lighty provides extremely strong extensibility.
- **Thread pool separation**: Supports Event-Loop and Pipeline using their own independent thread pools, and can specify the execution thread of each phase in the Pipeline on a 1-to-1 basis.
- **Load balancing**: Channels are assigned to Reactors using specific load balancing policies.
- **Buffer pool**: Supports buffer recycle and reuse to reduce memory overhead.
- **RingBuffer**: Uses double pointers for more convenient read and write operations.
- **Zero Copy**: Supports zero-copy file distribution.

## 📝Getting Started

```xml
<dependency>
 <groupId>io.github.light0x00</groupId>
 <artifactId>lighty-all</artifactId>
 <version>0.0.1</version>
</dependency>
```

> Ensure jdk version >= 17. if modularity is used, then the modules `lighty.core` and `lighty.codec` need to be required.

We will write a Hello World service that prints the received message and reply with 'Hello World'.

If we solve this problem in a top-down manner, then we can implement the upper level processing logic first, at this level we are dealing with input/output of type `String`, and the bottom layer is the encoding/decoding layer.

```txt
            ┌─────────────────────┐
 layer 3    │   MessageHandler    │
            └─────────────────────┘
                  ▲          │
                  │          ▼
            ┌────────┐  ┌─────────┐
 layer 2    │Decoder │  │ Encoder │
            └────────┘  └─────────┘
                  ▲          │
                  │          ▼
            ┌─────────────────────┐
 layer 1    │       Reactor       │
            └─────────────────────┘
```

Let's start by writing a `HelloWorldHandler` at layer 3 in the figure above.

```java
class HelloWorldHandler extends InboundChannelHandlerAdapter { //1.
  @Override
  public void onRead(@Nonnull ChannelContext context, @Nonnull Object data, @Nonnull InboundPipeline pipeline) { //2.
    //3.
    var msg = (String) data;
    System.out.println("Received:" + msg);
    //4.
    String reply = "Hello World";
    context.writeAndFlush(reply)
        //5.
        .addListener(future -> context.close());
  }
}
```

1. Extends `InboundChannelHandlerAdapter`.
2. Rewrite the `onRead` method to process the incoming data.
3. Convert the message to `String` and print it to the console.
4. Reply "Hello World".
5. After the sending is complete, close the channel.

Next, we need to implement the layer 2, fortunately Lighty has a built-in encoder/decoder for text, so we just need to add it to the pipeline.

```java
public class HelloWorldServer {
    public static void main(String[] args) {
        //1.
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //2.
        new ServerBootstrap()
                //3.
                .group(group)
                //4.
                .childInitializer(channel -> {
                    channel.pipeline().add(
                            new StringEncoder(StandardCharsets.UTF_8), //4.1
                            new StringDecoder(StandardCharsets.UTF_8), //4.2
                            new HelloWorldHandler());  //4.3
                })
                //5.
                .bind(new InetSocketAddress(9000));
    }
}
```

1. Create an event loop group with 1 thread, which means that we have 1 Reactor to handle the events of all the channels.
2. Create a Server.
3. Specify an event loop group.
4. Configure the Pipeline and specify the string encoder/decoder.
5. Specify the listening port.

At this point, we accomplished a service that accepts UTF-8 encoded text and responds with "Hello World." You can use telnet to debug, or you can continue to learn how write a client-side in Lighty.(See below)

## Postscript

If you want to learn more about Lighty use examples, you can read   [Best practices in Lighty](./doc/best-practices/index.en.md).

If you want know how it works, you can read  [Deep dive into Lighty](doc/deep-dive-into-lighty/index.en.md).

If you are interested in network framework, and want to discuss about it, you can join the [Slack group](https://join.slack.com/t/slack-o6y6551/shared_invite/zt-222eavevn-P75aH~I88F6Tq_g4gfQLmQ).
