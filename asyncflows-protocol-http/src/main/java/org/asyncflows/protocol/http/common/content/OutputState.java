package org.asyncflows.protocol.http.common.content;

/**
 * The state of output for the exchange.
 */
public enum OutputState {
    /**
     * The was no output yet, and headers are not yet written.
     */
    NOT_STARTED,
    /**
     * Data is being written, close of stream is an error.
     */
    DATA,
    /**
     * Data is being written, close of stream is possible.
     */
    DATA_CLOSEABLE,
    /**
     * The stream is in error.
     */
    ERROR,
    /**
     * The trailers were added or trailer are not needed (after close).
     */
    TRAILERS_ADDED,
    /**
     * The stream is closed.
     */
    CLOSED,
    /**
     * The stream is closed. No new messages can be written after that message.
     */
    CLOSED_LAST,
}
