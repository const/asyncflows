package org.asyncflows.protocol.http.common.content;

/**
 * The input state for the exchange.
 */
public enum InputState {
    /**
     * The continue reply is needed.
     */
    IDLE,
    /**
     * Data is being read, more data is possibly available.
     */
    DATA,
    /**
     * End of input data is reached, trailers might be available.
     */
    EOF,
    /**
     * End of input data is reached, trailers might be available.
     */
    EOF_NO_TRAILERS,
    /**
     * The trailers has been read.
     */
    TRAILERS_READ,
    /**
     * The input stream is closed.
     */
    CLOSED,
    /**
     * The input stream is closed before end of the data is reached. This usually means that connection
     * cannot be reused and should be closed after the current exchange finishes.
     */
    CLOSED_BEFORE_EOF,
    /**
     * The error happened, the connection should be closed.
     */
    ERROR,
}
