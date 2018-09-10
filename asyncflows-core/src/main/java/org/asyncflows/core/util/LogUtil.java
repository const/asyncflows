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
                    logger.debug(message.get() + " = " + resolution.value());
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
                    logger.debug(message + " = " + resolution.value());
                }
            }
        };
    }
}