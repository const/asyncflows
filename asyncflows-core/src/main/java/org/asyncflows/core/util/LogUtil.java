/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.core.util;

import org.asyncflows.core.function.AResolver;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * The utilities for the logger.
 */
public final class LogUtil {
    /**
     * Private constructor for utility class.
     */
    private LogUtil() {
        // do nothing
    }

    /**
     * The listener that logs failures on the specified logger with error level.
     *
     * @param logger  the logger
     * @param message the message to log.
     * @param <T>     the resolver type
     * @return the logging resolver
     */
    public static <T> AResolver<T> logFailures(final Logger logger, final String message) {
        return resolution -> {
            if (resolution.isFailure() && logger.isErrorEnabled()) {
                logger.error(message, resolution.failure());
            }
        };
    }

    /**
     * The listener that logs failures on the specified logger with debug level.
     *
     * @param logger  the logger
     * @param message the message to log.
     * @param <T>     the resolver type
     * @return the logging resolver
     */
    public static <T> AResolver<T> logDebugFailures(final Logger logger, final String message) {
        return resolution -> {
            if (resolution.isFailure() && logger.isDebugEnabled()) {
                logger.debug(message, resolution.failure());
            }
        };
    }

    /**
     * This method is used for debug logging in case when it is required to observe results and log it.
     *
     * @param logger  the logger
     * @param message the message to log.
     * @param <T>     the resolver type
     * @return a resolver that logs a checkpoint on debug. It is used to statements to logs.
     */
    public static <T> AResolver<T> checkpoint(final Logger logger, final Supplier<String> message) {
        return resolution -> {
            if (logger.isDebugEnabled()) {
                if (resolution.isFailure()) {
                    logger.debug(message.get(), resolution.failure());
                } else {
                    logger.debug(String.format("%s = %s", message.get(), resolution.value()));
                }
            }
        };
    }

    /**
     * This method is used for debug logging in case when it is required to observe results and log it.
     *
     * @param logger  the logger
     * @param message the message to log.
     * @param <T>     the resolver type
     * @return a resolver that logs a checkpoint on debug. It is used to statements to logs.
     */
    public static <T> AResolver<T> checkpoint(final Logger logger, final String message) {
        return resolution -> {
            if (logger.isDebugEnabled()) {
                if (resolution.isFailure()) {
                    logger.debug(message, resolution.failure());
                } else {
                    logger.debug(String.format("%s = %s", message, resolution.value()));
                }
            }
        };
    }
}
