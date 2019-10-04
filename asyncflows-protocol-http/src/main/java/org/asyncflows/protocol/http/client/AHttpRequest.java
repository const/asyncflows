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

package org.asyncflows.protocol.http.client;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.io.AOutput;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * The HTTP request interface.
 *
 * @see org.asyncflows.protocol.http.common.HttpScopeUtil for addtional keys.
 */
@Asynchronous
public interface AHttpRequest extends ACloseable {
    /**
     * @return the remote address for the socket (hop only)
     */
    Promise<SocketAddress> getRemoteAddress();

    /**
     * @return the remote address for the socket (hop only)
     */
    Promise<SocketAddress> getLocalAddress();

    /**
     * Connect the specified server.
     *
     * @param scope   the scope that is used to provide additional parameters to the request .
     * @param method  the request method
     * @param uri     the url to connect to
     * @param headers the request headers
     * @param length  the content length or null ({@link HttpRequestUtil#NO_CONTENT} value means that there is no content)
     * @return a promise for the output stream associated with the request.
     */
    Promise<AOutput<ByteBuffer>> request(Scope scope, String method, URI uri,
                                         HttpHeaders headers, Long length);

    /**
     * @return the promise for HTTP response, when response type becomes finally known.
     */
    Promise<HttpResponse> getResponse();
}
