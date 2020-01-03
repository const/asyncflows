/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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
