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
