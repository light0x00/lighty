
关于 Buffer

- 读写隔离
- 可重用,缓冲池 
  - 可虚拟 capacity
  - generation 机制

关于并发

- 对于一个 channel 的读写、编码解码、报文处理,每一个阶段都可以指定不同的 `Executor` 执行, 但是对于每一个阶段,每次执行,执行线程应该总是不变. 这样可以避免线程安全问题.

---

- netty 是如何优雅 shutdown ,
- netty 在 readable 事件中, ByteBuffer 的缓存设计


## Netty 中的启发

### 连接关闭

netty 中, 如果一方调用 ctx.close() , 那么这一方的写缓冲区中的待写数据将丢弃 `AbstractUnsafe#close:735`. 
而当另一方 read() 返回 -1 时, 会根据配置 `isAllowHalfClosure` 决定是否关闭 socket, 当选择关闭时 写缓冲区中的数据会被丢弃 `NioByteUnsafe#closeOnRead:101` 

> 被丢弃的数据对应的 future 回调会返回异常 StacklessClosedChannelExceptionΩ

### 事件循环 shutdown 

事件循环组,将状态

清除 selector
执行完剩余任务


```java
  if ((interestOps & SelectionKey.OP_WRITE) == 0) {
  key.interestOps(interestOps | SelectionKey.OP_WRITE);
  }
  ch.isOpen() && ch.isConnected();
```
