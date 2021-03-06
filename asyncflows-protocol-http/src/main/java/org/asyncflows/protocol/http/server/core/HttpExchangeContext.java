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

package org.asyncflows.protocol.http.server.core;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.server.HttpExchange;

import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aNow;

/**
 * HTTP exchange for the server side. This object is handled by the server to the request handlers.
 * The object provides most basic interface that allows handling HTTP protocol with maximum flexibility.
 * MVC frameworks are supposed to use it and work above it. The object is not thread safe, except for scopes,
 * that could be used in thread safe manner. So it must be passed either in the token manner (only one vat uses it),
 * or access to it should be protected by other means.
 */
class HttpExchangeContext implements HttpExchange {
    // TODO validate parameters (status, headers, etc)
    /**
     * The server scope that allows sharing information on the server.
     */
    private final Scope serverScope;
    /**
     * The exchange scope.
     */
    private final Scope exchangeScope = new Scope();
    /**
     * The method.
     */
    private final String method;
    /**
     * The request headers.
     */
    private final HttpHeaders requestHeaders;
    /**
     * The input (null if there is no input).
     */
    private final AInput<ByteBuffer> input;
    /**
     * The read trailers action.
     */
    private final ASupplier<HttpHeaders> trailers;
    /**
     * The tracker facet for HTTP exchange. It provides methods used by this utility class.
     */
    private final AHttpResponse response;
    /**
     * The request URL.
     */
    private final URI requestUri;
    /**
     * The local address for the exchange.
     */
    private final SocketAddress localAddress;
    /**
     * The remote address for teh exchange.
     */
    private final SocketAddress remoteAddress;
    /**
     * The input length (if defined).
     */
    private final Long inputLength;

    /**
     * The constructor.
     *
     * @param serverScope    the server scope object
     * @param method         the request method
     * @param requestHeaders the request headers
     * @param input          the input stream
     * @param trailers       the trailers action
     * @param response       the response tracker
     * @param requestUri     the request URL
     * @param localAddress   the local address
     * @param remoteAddress  the remote address
     * @param inputLength    the input length
     */
    @SuppressWarnings("squid:S00107")
    public HttpExchangeContext(final Scope serverScope,
                               final String method, final HttpHeaders requestHeaders, final AInput<ByteBuffer> input,
                               final ASupplier<HttpHeaders> trailers,
                               final AHttpResponse response, final URI requestUri, final SocketAddress localAddress,
                               final SocketAddress remoteAddress, final Long inputLength) {
        this.serverScope = serverScope;
        this.method = method;
        this.requestHeaders = requestHeaders;
        this.input = input;
        this.trailers = trailers;
        this.response = response;
        this.requestUri = requestUri;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.inputLength = inputLength;
    }

    @Override
    public Scope getExchangeScope() {
        return exchangeScope;
    }


    @Override
    public Scope getServerScope() {
        return serverScope;
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return requestHeaders;
    }


    @Override
    public AInput<ByteBuffer> getInput() {
        return input;
    }

    @Override
    public Long getInputLength() {
        return inputLength;
    }

    @Override
    public Promise<Void> intermediateResponse(final int status, final String reason, final HttpHeaders headers) {
        return response.intermediateResponse(status, reason, new HttpHeaders(headers));
    }

    @Override
    public Promise<AOutput<ByteBuffer>> respond(final int status, final String reason,
                                                final HttpHeaders headers, final Long length) {
        return response.respond(status, reason, new HttpHeaders(headers), length);
    }


    @Override
    public Promise<AOutput<ByteBuffer>> respond(final int status, final HttpHeaders headers, final Long length) {
        return respond(status, null, headers, length);
    }

    @Override
    public Promise<AOutput<ByteBuffer>> respond(final int status, final HttpHeaders headers) {
        return respond(status, headers, null);
    }

    @Override
    public Promise<HttpHeaders> readTrailers() {
        return aNow(trailers);
    }

    @Override
    public Promise<AChannel<ByteBuffer>> switchProtocol(final int status, final String reason,
                                                        final HttpHeaders headers) {
        return response.switchProtocol(status, reason, new HttpHeaders(headers));
    }


    @Override
    public Promise<Void> close() {
        return response.close();
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public URI getRequestUri() {
        return requestUri;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return localAddress;
    }
}
