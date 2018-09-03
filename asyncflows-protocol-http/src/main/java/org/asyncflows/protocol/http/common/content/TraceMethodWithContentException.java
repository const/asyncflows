package org.asyncflows.protocol.http.common.content;

import org.asyncflows.protocol.http.HttpException;

/**
 * This exception is thrown when trace method with content is encountered.
 * This usually indicates some bug on the client side.
 */
public class TraceMethodWithContentException extends HttpException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public TraceMethodWithContentException(final String message) {
        super(message);
    }
}
