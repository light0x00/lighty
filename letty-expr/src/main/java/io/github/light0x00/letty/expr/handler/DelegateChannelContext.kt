package io.github.light0x00.letty.expr.handler

/**
 * @author light0x00
 * @since 2023/7/10
 */
open class DelegateChannelContext(target: ChannelContext) : ChannelContext by target