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

package org.asyncflows.protocol.http.common;

import org.asyncflows.core.function.ASupplier;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.common.headers.TransferEncoding;

import javax.net.ssl.SSLSession;
import java.util.List;

import static org.asyncflows.core.function.AsyncFunctionUtil.constantSupplier;

/**
 * The common keys used for client and server to indicate specific protocol features.
 */
public final class HttpScopeUtil {
    /**
     * Set this key to true on the exchange scope before responding in order to ensure that this exchange is
     * the last on the connection. Usually there is no reason to force closing connection, since the container
     * does it automatically when appropriate.
     */
    public static final Scope.Key<Boolean> LAST_EXCHANGE = new Scope.Key<>(HttpScopeUtil.class, "lastExchange", false);
    /**
     * The key used to force a specific version of http protocol. In practice it could be used only to force HTTP/1.0,
     * since for HTTP/2.0 other ways are used.
     */
    public static final Scope.Key<String> FORCE_VERSION = new Scope.Key<>(HttpScopeUtil.class, "forceVersion");
    /**
     * The names of trailers.
     */
    public static final Scope.Key<List<String>> TRAILERS_NAMES = new Scope.Key<>(HttpScopeUtil.class, "trailersNames");
    /**
     * The provider for trailers.
     */
    public static final Scope.Key<ASupplier<HttpHeaders>> TRAILERS_PROVIDER = new Scope.Key<>(
            HttpScopeUtil.class, "trailersProvider");
    /**
     * The SSL session.
     */
    public static final Scope.Key<SSLSession> SSL_SESSION = new Scope.Key<>(HttpScopeUtil.class, "sslSession");

    /**
     * The private constructor.
     */
    private HttpScopeUtil() {

    }

    /**
     * Get trailers provider basing on the scope.
     *
     * @param scope     the scope
     * @param headers   the headers
     * @param encodings the encodings
     * @return the trailers provider
     */
    public static ASupplier<HttpHeaders> trailersProvider(final Scope scope,
                                                          final HttpHeaders headers,
                                                          final List<TransferEncoding> encodings) {
        if (TransferEncoding.isChunked(encodings)) {
            final ASupplier<HttpHeaders> provider = scope.get(TRAILERS_PROVIDER);
            if (provider != null) {
                final List<String> names = scope.get(TRAILERS_NAMES);
                if (names != null) {
                    headers.setHeader(HttpHeadersUtil.TRAILER_HEADER, HttpHeadersUtil.listValue(names));
                }
                return provider;
            }
        }
        return constantSupplier(null);
    }

}
