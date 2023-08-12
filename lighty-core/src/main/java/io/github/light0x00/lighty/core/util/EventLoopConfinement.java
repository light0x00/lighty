package io.github.light0x00.lighty.core.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 *
 * Indicate the code block must be executed only in current event loop.
 *
 * @author light0x00
 * @since 2023/8/12
 */
@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface EventLoopConfinement {
}
