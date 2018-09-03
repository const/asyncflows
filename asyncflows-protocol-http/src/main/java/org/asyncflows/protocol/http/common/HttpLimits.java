package org.asyncflows.protocol.http.common;

/**
 * The HTTP protocol limits for the library. They will be possibly made configurable in the future.
 */
public final class HttpLimits {
    /**
     * The maximum size of start line.
     */
    public static final int MAX_START_LINE_SIZE = 10 * 1024;
    /**
     * The maximum size of headers.
     */
    public static final int MAX_HEADERS_SIZE = 100 * 1024;
    /**
     * The default size of the buffer.
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;
    /**
     * The maximum size of line when reading the chunks (to prevent DDoS).
     */
    public static final int MAX_CHUNK_LINE = 4096;
    /**
     * The default timeout of client connections, after which they are closed.
     */
    public static final int DEFAULT_IDLE_CONNECTION_TIMEOUT = 60 * 1000;

    /**
     * The private constructor.
     */
    private HttpLimits() {
        // do nothing
    }
}
