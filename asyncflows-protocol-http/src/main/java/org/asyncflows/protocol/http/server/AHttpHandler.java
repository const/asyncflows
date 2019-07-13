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

package org.asyncflows.protocol.http.server;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;

/**
 * The basic HTTP handler interface.The more complex interfaces are supposed to be built upon it.
 * The handler provides access to the very raw HTTP request.
 */
@Asynchronous
public interface AHttpHandler {
    /**
     * Handle the exchange. After the result promise is resolved, the both input and output are closed. If input
     * has not been fully read, the HTTP connection might be aborted, and not handle next requests pipelined
     * in it. If output is with the specified content length, it is closed. If the protocol switch happened,
     * the underlying channel is closed with the method finishes.
     *
     * @param exchange the exchange
     * @return a promise that resolves when request is completely handled.
     */
    Promise<Void> handle(HttpExchange exchange);
}
