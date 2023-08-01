package io.github.light0x00.letty.core.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 为子类添加 log 扩展字段
 *
 * @author light0x00
 * @since 2023/7/31
 */
interface Loggable {

}

val Loggable.log: Logger
    get() {
        return LoggerFactory.getLogger(this::class.java);
    }
