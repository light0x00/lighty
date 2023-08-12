[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven
Central](https://maven-badges.herokuapp.com/maven-central/io.github.light0x00/lighty/badge.svg)
](https://repo1.maven.org/maven2/io/github/light0x00/lighty/)[![Java support](https://img.shields.io/badge/Java-17+-green?logo=java&logoColor=white)](https://openjdk.java.net/)

<p align="center">
    <img src="doc/logo.png" alt="...">
</p>

中文 | [English](./README.en.md)

Lighty is medium-performance, lightweight, non-blocking, even-driven network framework. Some of its design inspired by Netty, but completely different implementation.

> Why is Lighty only medium-performance?
> 
> Although some of the efforts has been made in aspect of performance, like the pooled buffer, ring buffer, lock-free tricks, thread-confinement, etc. There are still several points that can be optimized. (See the issues in detail)

```txt
                                                                            inbound handlers pipeline

 ┌────────────┐              ┌────────────┐              ┌─────────────────────────────────────────────────────────────┐
 │            │   readable   │            │  input data  │ ┌──────────┬──────────┬─────────┬─────────────┬───────────┐ │
 │            │   ─ ─ ─ ─ ►  │            │   ───────►   │ │ handler1 │ handler2 │ 。。。   │ handlerN-1  │  handlerN │ │
 │            │              │            │              │ └──────────┴──────────┴─────────┴─────────────┴───────────┘ │
 │            │              │            │              └─────────────────────────────────────────────────────────────┘
 │            │              │ event      │
 │            │              │            │                                           │
 │ event loop │              │ dispatcher │                                           │ write
 │            │              │            │                                           ▼
 │            │              │            │              ┌─────────────────────────────────────────────────────────────┐
 │            │  writeable   │            │ output data  │ ┌──────────┬────────────┬─────────┬───────────┬───────────┐ │
 │            │  ◄ ─ ─ ─ ─   │            │   ◄───────   │ │ handlerM │ handlerM-1 │ 。。。   │ handler2  │  handler1 │ │
 │            │              │            │              │ └──────────┴────────────┴─────────┴───────────┴───────────┘ │
 └────────────┘              └────────────┘              └─────────────────────────────────────────────────────────────┘

                                                                             outbound handlers pipeline
```


## 