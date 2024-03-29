- BufferPool
    - [x] BufferPool 抽象
    - [x] BufferPool 的 DirectByteBuffer 的回收问题
    - [x] BufferPool LRU
    - [ ] LruBufferPool 中的链表, 去掉泛型, 使用基础类型 int, 避免装箱成本
- [x] EventLoop shutdown
- [x] 异常处理,异常捕获, 需要考虑: 哪些异常应该转给用户? [issue0000]
- [x] 门面向 netty 对齐, 比如
    - 对用户不区分 inboundHandler、outboundHandler , 内部再区分,
    - 比如设置 socket 的属性
    - ChannelInitializer 的方式传入 channel 配置
- [x] 责任链调用、观察者通知 支持按1对1指定 executor
- [ ] ListenableFutureTask 需要拆分为接口和实现 ,为避免目前 `run`
  方法暴露给用户,返回给用户的应为受限的代理对象,用户调用该对象的 `run` 将抛出异常
- [x] Outbound Buffer 分离为单独组件, 内部统计字节数量
- [x] 对比 close、shutdownInput、shutdownOutput 在 TCP 层面的区别
- [x] 解决优雅关闭 channel 时双向(input、output)检测,都 shutdown 时将 channel 的 key cancel
- [x] 看 Netty shutdownGracefully 源码实现 (紧急)
- [x] 将生命周期的几个 future 移入 ChannelEventNotifier 类, 然后用用户的 executor 去执行, 而不是 event loop 线程
- [x] 调研 当 ServerSocketChannel close 后, 其 accept 的 SocketChannel 以及 SelectionKey 的状态,
  考虑是否需要释放资源 [issue0001]
- [x] 支持文件分发场景的零拷贝, 调试 `FileChannel.transferTo` 源码
- [x] 考虑 nio event loop 中, catch 到异常, 是否需要执行相关 handler 的释放资源操作
- [ ] 每当 channel 读事件发生, 要分配内存装载就绪的数据时, 对缓冲区大小的动态统计和预测能力.
    - 比如一开始每次分配 1kb 的读缓冲区, 但是每次都需要分几次才能读完 channel, 那么意味着缓冲区分配小了, 下一次应该分配更大的缓冲区.
    - 而另一面, 如果每次装载就绪数据时, 缓冲区都有很多剩余空间, 则意味着缓冲区大了, 下次应该分配更小的.
  > 实现此功能的目的在于, 减少系统调用的次数, 提升性能.
  > 这种弹性能力, 能更好的适配应用层协议的特征. 比如大文件传输协议, 往往数据总是大批量的到来. 而 rpc 协议, 数据总是小段的到来.
- [x] 区分 write 和 flush, 目前的实现每次 write 都会唤醒一次 nio 线程, 应该为只有 flush 或者 write 的数据量足够多才唤醒
- [ ] 写入 OutputBuffer 的数据到 channel 时, 采用合并写
- [x] 支持对 socket 的配置, 比如 tcp 的 receive buffer size
- [x] 依靠 channel 的生命周期是不够的, 在连接失败(如 Connection refused)时, onConnected/onClosed? 都不会触发, 需要实现 初始化、销毁的生命周期勾子
- [x] SocketChannelEventHandler 作为抽象类, 作为模版方法基类，让子类 Server/Client 继承  
- [x] ChannelContext 直接实现 NioSocketChannel
- [x] 入方向 pipeline 返回 future
- [x] 零拷贝不经过 outbound pipeline
- [ ] StringDecoder MalformedInputException、UnmappableCharacterException 问题, 考虑这种情况是否应该替换为错误字符
- [ ] 将 EventLoop、BufferPool 存到 ThreadLocal, `ListenableFutureTask` notifier 默认使用当前 EventLoop 
- [ ] 支持设置 backlog 
- [ ] 实现链式 RingBuffer
- [ ] EventLoop 访问级别的资源放在 ThreadLocal, 如 BufferPool 
- [ ] 实现连接超时、读超时, 研究下 netty 的实现
  1. `ReadTimeoutHandler` 实现原理 
  2. `ChannelOption.CONNECT_TIMEOUT_MILLIS` 在服务端和客户端的实现原理
