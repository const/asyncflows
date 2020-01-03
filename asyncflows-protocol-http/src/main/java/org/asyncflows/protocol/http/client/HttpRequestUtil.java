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

package org.asyncflows.protocol.http.client;

import org.asyncflows.core.function.AResolver;
import org.asyncflows.protocol.http.common.Scope;

/**
 * Utilities and constants related to {@link HttpRequestUtil}.
 */
public final class HttpRequestUtil {
    /**
     * The continue listener. If it the version of the request is 1.1, then the listener is notified when the
     * continue intermediate reply is received. Note that this reply might never be received in case if the server
     * does not understand this header, so the client should watch some timer to check for the response.
     */
    public static final Scope.Key<AResolver<Void>> CONTINUE_LISTENER
            = new Scope.Key<>(AHttpRequest.class, "continueListener");
    /**
     * Set this key on the scope, if the request should connect to the host, that is different from the host specified
     * by URL.
     */
    public static final Scope.Key<String> CONNECTION_HOST = new Scope.Key<>(AHttpRequest.class, "connectionHost");
    /**
     * The constant meaning no-content.
     */
    public static final long NO_CONTENT = -1L;

    private HttpRequestUtil() {
    }
}
