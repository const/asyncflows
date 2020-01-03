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

package org.asyncflows.protocol.http.server;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * The interface represent a context of the single HTTP exchange.
 * Note that it is a low level interface. The higher-level API
 * is supposed to be provided over it.
 */
public interface HttpExchange extends ACloseable {

    /**
     * Get request scope, the scope is available for the lifetime of the request. It could be used to store information
     * by filters along to request handling chain.
     *
     * @return the request scope
     */
    Scope getExchangeScope();

    /**
     * Get request scope, the scope is available for the lifetime of the request. It could be used to store information
     * by filters along to request handling chain.
     *
     * @return the request scope
     */
    Scope getServerScope();

    /**
     * @return the request method
     */
    String getMethod();

    /**
     * The effective request URL represents all information needed to route the request. This information is computed
     * according to <a href="http://tools.ietf.org/search/rfc7230#section-5.5">RFC 7230, Section 5.5</a>.
     * The information about path, host, and protocol should be taken from this URL.
     *
     * @return the effective request URL
     */
    URI getRequestUri();

    /**
     * @return the remote IP address (this is a hop address, so it might return a proxy address)
     */
    SocketAddress getRemoteAddress();

    /**
     * @return the local IP address (this is a hop address, so it returns address beyond a nat in
     * the case of port forwarding).
     */
    SocketAddress getLocalAddress();

    /**
     * @return the request headers
     */
    HttpHeaders getRequestHeaders();

    /**
     * Get input stream. When the first read request is scheduled for input, "101 continue" request
     * is sent for HTTP 1.1 case if needed.
     *
     * @return the input, always non-null but might be zero-size.
     */
    AInput<ByteBuffer> getInput();

    /**
     * @return the length of input, if it is known.
     */
    Long getInputLength();

    /**
     * Do intermediate response (except for protocol switching). In practice, "100 continue" is sent automatically,
     * as soon as input starts to be read. So intermediate response could be just "102 processing" from WebDAV,
     * or some other extension over HTTP.
     *
     * @param status  the status code
     * @param reason  the status reason (null for the default text)
     * @param headers the headers
     * @return the promise for output stream that correspond to content mode the promise might resolve to null,
     * if the response should not have a content basing on request and .
     */
    Promise<Void> intermediateResponse(int status, String reason, HttpHeaders headers);

    /**
     * Start responding. This is most generic version of the response method.
     *
     * @param status  the status code`
     * @param reason  the status reason (null for the default text)
     * @param headers the headers
     * @param length  the length or null
     * @return the promise for output stream that correspond to content mode the promise might resolve to null,
     * if the response should not have a content basing on request or response code.
     */
    Promise<AOutput<ByteBuffer>> respond(int status, String reason, HttpHeaders headers, Long length);

    /**
     * Start responding with the default reason message.
     *
     * @param status  the status line
     * @param headers the headers
     * @param length  the length or null
     * @return the promise for output stream that correspond to content mode the promise might resolve to null,
     * if the response should not have a content basing on request and .
     */
    Promise<AOutput<ByteBuffer>> respond(int status, HttpHeaders headers, Long length);

    /**
     * Start responding with unknown length and default status message.
     *
     * @param status  the status line
     * @param headers the headers
     * @return the promise for output stream that correspond to content mode the promise might resolve to null,
     * if the response should not have a content basing on request and .
     */
    Promise<AOutput<ByteBuffer>> respond(int status, HttpHeaders headers);

    /**
     * Read trailers.
     *
     * @return the promise that resolves when trailers are available or it is known that they will not be available.
     * the promise might resolve to null, if trailers are not supported by the current entity.
     */
    Promise<HttpHeaders> readTrailers();

    /**
     * In case when HTTP connect request is handled or protocol upgrade happened, this method allows to receive
     * the rest of the underlying connection for the switched protocol after replying normally. The connection
     * returns only after the both input and output are closed, so they would not interfere.
     *
     * @param status  the status line
     * @param reason  the status reason (null for the default text)
     * @param headers the headers
     * @return a channel for the switched protocol
     */
    Promise<AChannel<ByteBuffer>> switchProtocol(int status, String reason, HttpHeaders headers);

    // TODO HTTP2 push: Promise<Void> pushRequest(URI uri) ?
}
