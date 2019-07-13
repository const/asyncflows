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
