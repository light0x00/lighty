- 整体流程(基于流程图介绍)
- event loop 设计
    - 线程池改造
        - lock free技巧,如 shutdown、execute
    - event loop group 负载均衡
    - listenable future task 异步化
- 缓存池设计
    - LRU
    - RingBuffer
    - release 机制(读写锁解决并发问题)
- ChannelHandler 设计(责任链、观察者、写缓冲队列)
    - OutboundPipeline 中执行 write操作, 如何避免死循环问题.
- 边界情况处理
    - 连接关闭,半开连接(一方关闭)

---

## 并发问题

### Channel shutdown 操作

#### 动机

当一个 socket 的输入端、输出端均被关闭时(通过 `shutdownInput/shutdownOutput`), 其对应 `SelectionKey`
仍旧存在于 `Selector` 的 key set中, 并且 channel 会处于不一致状态(isConnected、sOpen 均为 true) , 等一些释放资源操作无法被执行.
这是不可容忍的, 需要一种机制探测 input/output 都关闭的情况.

#### 解决方案

逻辑上,解决这个问题并不难, 只需要

- 在发现 socket input 被关时检查一下 output 是否被关
- 在发现 socket output 被关时检查一下 input 是否被关
  以上两种情况,任意一种条件为 true 则意味 socket 已经关闭, 可以回收资源, 伪代码实现如下:

```kotlin
var shutdownInput = false
var shutdownOutput = false

fun shutdownInput() {
    shutdownInput = true
    if (shutdownOutput == true) {
        releaseResource();
    }
}

fun shutdownOutput() {
    shutdownOutput = true
    if (shutdownInput == true) {
        releaseResource()
    }
}
```

代码很简单, 在单线程环境, 是可以正确工作的. 

那么在并环境下, 执行结果能正确吗? 如果不正确, 应采取什么样的同步方案? 如何避免“过度同步”? 

这是我们在本节要探讨的问题. 我们将前述代码表示为下图:

```plantuml
@startuml
hide empty description

state "线程A" as A {
  state "操作1: shutdownInput = true" as 1
  state "操作2: r1 = shutdownOutput " as 2
  state "操作3: If r1 == true" as 3
  
  state "releaseResource()" as 4
  
    1-->2
    2-->3
    3-->4 : yes
}

state "线程B" as B {
    state "操作1: shutdownOutput = true" as B1
    state "操作2: r2 = shutdownInput" as B2
    state "操作3: if r2 == true" as B3
    state "releaseResource()" as B4
    
    B1-->B2
    B2-->B3
    B3-->B4 : yes
}

@enduml
```

由于变量 `shutdownInput`,`shutdownOutput` 的读写处于 "Relaxed Mode", 两个线程的操作1 和 操作2 之间并没依赖关系, 重排是允许的.  

> 这里所说的 “Relaxed Mode” 对应于 JEP188 Java Memory Model 中的 “Plain Mode”. ( C++11 的 `memory_order_relaxed`)
> 
> 引用 Doug Lea 在 《Using JDK 9 Memory Order Modes》中的段落:
> - 编译器重排:
> Compilers are allowed to make some different choices when mapping the original tree-like (and parallelizable) expression into a sequential instruction stream.
> 
> - 处理器重排:
> Such decisions by compilers about instruction order might not matter much, because commodity processors themselves perform parallel ("out-of-order") instruction execution.

也即是, 肯能发生如下执行时序:

```plantuml
@startuml
title: 重排序

|t1|线程A
|t2|线程B

split
-[hidden]->

|t1|
:操作2: r1=shutdownOutput;
:操作1: shutdownInput=true;
:操作3: if r1==true;
->yes;
:releaseResource();

detach;

split again
-[hidden]->

|t2|
:操作2: r2=shutdownInput;
:操作1: shutdownOutput=true;
:操作3: if r2==true;
->yes;
:releaseResource();

detach;
end split

@enduml
```

1. 线程A 和 线程B 在同一时刻,分别读取 shutdownOutput、shutdownInput, 此时结果都为 false
2. 线程A 和 线程B 分别更新 shutdownOutput、shutdownInput 的值为 true
3. 由于第一步中, 读到的值为 false, 线程A 和 线程B都不会执行 `releaseResource`

至此, 问题已经很明确, 我们要做的就是阻止重排.

熟悉 [JSR133](https://www.cs.umd.edu/~pugh/java/memoryModel/jsr133.pdf) 的人应该会想到利用 volatile 的内存语义, 其具体依据是 《JSR-133: JavaTM Memory Model and Thread Specification》 中的如下条款:

> The semantics of volatile variables have been strengthened to have acquire and release semantics. In the original specification, accesses to volatile and non-volatile variables could be freely ordered.

如果给 


```java
boolean outputFin;
boolean inputFin;

public void shutdownInput() {
  inputFin = true; //写操作
  if (outputFin) { //读操作
    releaseResource();
  }
}

public void shutdownOutput() {
  outputFin = true;   //写操作
  if (inputFin) {     //读操作
    releaseResource();
  }
}
```


```plantuml
@startuml
start
split
   :A;
split again
   :B;
split again
   :C;
split again
   :a;
   :b;
end split
:D;
end
@enduml

```

```plantuml
@startuml

split
-[hidden]->
group shutdownInput
:shutdownInput=true;
:If shutdownOutput==true
   channel.close()
   key.cancel();

end group

detach


split again
-[hidden]->

group shutdownOutput
:shutdownOutput=true;
:StoreLoad;
:if shutdownInput==true
   channel.close()
   key.cancel();
end group
   
detach

end split
```

```plantuml
@startuml


split
-[hidden]->

group 线程A
:input=false;
:if output=false;
:close();
end group

detach



split again
-[hidden]->

group 线程B
:output=false;
:if input=false;
:close();
end group
   
kill
end split


@enduml
```

```plantuml
@startuml
|Swimlane1|
start
:foo1;
|#AntiqueWhite|Swimlane2|
:foo2;
:foo3;
|Swimlane1|
:foo4;
|Swimlane2|
:foo5;
stop
@enduml
```

## 2
```java


```

```txt
Thread1            | Thread2
---------------------------------------
shudownOutput=true | write  
clearOutputBuffer  | if shudownOutput!=true
                   | then append OutputBuffer
 

```