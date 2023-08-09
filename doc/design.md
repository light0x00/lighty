## 关于 Buffer

- 读写隔离
- 可重用,缓冲池
    - 可虚拟 capacity
    - generation 机制

## 关于并发

- 对于一个 channel 的读写、编码解码、报文处理,每一个阶段都可以指定不同的 `Executor` 执行, 但是对于每一个阶段,每次执行,执行线程应该总是不变.
  这样可以避免线程安全问题.

## 关于NIO

无意义的 interest 应及时从 interest set 中移除, 比如 对于一个 channel 而言 connectable
事件发生一次后就不会再发生了,应 `key.interestOps() ^ SelectionKey.OP_CONNECT` 移除掉, 否则会导致 `selector.select()`
无限返回 0.

## Netty 中的启发

### 连接关闭

netty 中, 如果一方调用 ctx.close() , 那么这一方的写缓冲区中的待写数据将丢弃 `AbstractUnsafe#close:735`.
而当另一方 read() 返回 -1 时, 会根据配置 `isAllowHalfClosure` 决定是否关闭 socket, 当选择关闭时
写缓冲区中的数据会被丢弃 `NioByteUnsafe#closeOnRead:101`

> 被丢弃的数据对应的 future 回调会返回异常 StacklessClosedChannelExceptionΩ

### 事件循环 shutdown

TODO

清除 selector
执行完剩余任务

### Buffer 回收

写 buffer 回收机制

读 buffer 回收机制

### 异常处理

连接意外断开

用户代码导致异常(是否中断事件循环)


