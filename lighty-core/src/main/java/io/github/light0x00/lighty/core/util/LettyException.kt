package io.github.light0x00.lighty.core.util

import org.slf4j.helpers.MessageFormatter

/**
 * @author light0x00
 * @since 2023/7/11
 */
class LettyException(message: String?) : RuntimeException(message) {

    constructor(pattern: String?, vararg args: Any) : this(MessageFormatter.arrayFormat(pattern, args).message)

}