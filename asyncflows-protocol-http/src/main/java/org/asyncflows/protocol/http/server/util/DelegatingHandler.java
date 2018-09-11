/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.protocol.http.server.util;

import org.asyncflows.protocol.http.server.AHttpHandler;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.core.ServerOptionsHandler;
import org.asyncflows.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The simple delegating handler, it is mostly used for testing.
 */
public class DelegatingHandler extends HttpHandlerBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DelegatingHandler.class);
    /**
     * The delegates.
     */
    private final List<Delegate> delegates = new ArrayList<>();
    /**
     * The fallback handler.
     */
    private AHttpHandler fallback;

    /**
     * The constructor.
     *
     * @param fallback the fallback handler
     */
    public DelegatingHandler(final AHttpHandler fallback) {
        this.fallback = fallback;
    }

    /**
     * The constructor with default fallback (404 on all requests).
     */
    public DelegatingHandler() {
        this(defaultFallback());
    }

    /**
     * @return the default fallback.
     */
    private static AHttpHandler defaultFallback() {
        final DelegatingHandler handler = new DelegatingHandler(new NotFoundHandler());
        handler.addDelegate(new PathDelegate("OPTIONS", null, "", new ServerOptionsHandler()));
        return handler;
    }

    /**
     * Add delegate to the list.
     *
     * @param delegate the delegate
     */
    public void addDelegate(final Delegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        for (final Delegate delegate : delegates) {
            final AHttpHandler handler;
            try {
                handler = delegate.getHandler(exchange);
            } catch (Throwable e) {
                LOG.error("Failed to check if delegate is applicable", e);
                continue;
            }
            if (handler != null) {
                return handler.handle(exchange);
            }
        }
        return getFallback().handle(exchange);
    }

    /**
     * @return the current fallback handler.
     */
    public AHttpHandler getFallback() {
        return fallback;
    }

    /**
     * Set fallback handler.
     *
     * @param fallback the handler
     */
    public void setFallback(final AHttpHandler fallback) {
        this.fallback = fallback;
    }

    /**
     * The delegate information.
     */
    public interface Delegate {
        /**
         * Check if the request could be handled by this handler. Even if the registration fails,
         * it still could enrich the scope object.
         *
         * @param exchange the exchange
         * @return the handler if the request could be handled, null if the next handler should be tried.
         */
        AHttpHandler getHandler(HttpExchange exchange);
    }

    /**
     * The prefix based delegation. It checks host and path prefix.
     */
    public static final class PrefixPathDelegate implements Delegate {
        /**
         * The HTTP method.
         */
        private final String method;
        /**
         * The authority.
         */
        private final String authority;
        /**
         * The path prefix.
         */
        private final String prefix;
        /**
         * The handler.
         */
        private final AHttpHandler handler;

        /**
         * The constructor.
         *
         * @param method    the method
         * @param authority the authority (host and port)
         * @param prefix    the prefix
         * @param handler   the handler
         */
        public PrefixPathDelegate(final String method, final String authority, final String prefix,
                                  final AHttpHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("The handler must not be null");
            }
            this.method = method;
            this.authority = authority;
            this.prefix = prefix;
            this.handler = handler;
        }

        @Override
        public AHttpHandler getHandler(final HttpExchange exchange) {
            final URI requestUri = exchange.getRequestUri();
            if (authority != null && !authority.equalsIgnoreCase(requestUri.getAuthority())) {
                return null;
            }
            final String path = requestUri.getPath();
            if (prefix != null && path != null
                    && !requestUri.getPath().startsWith(prefix)) {
                return null;
            }
            if (method != null && !method.equals(exchange.getMethod())) {
                return null;
            }
            return handler;
        }
    }

    /**
     * The prefix based delegation. It checks host and path prefix.
     */
    public static final class PathDelegate implements Delegate {
        /**
         * The HTTP method.
         */
        private final String method;
        /**
         * The authority.
         */
        private final String authority;
        /**
         * The path prefix.
         */
        private final String path;
        /**
         * The handler.
         */
        private final AHttpHandler handler;

        /**
         * The constructor.
         *
         * @param method    the method
         * @param authority the authority (host and port)
         * @param path      the path
         * @param handler   the handler
         */
        public PathDelegate(final String method, final String authority, final String path,
                            final AHttpHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("The handler must not be null");
            }
            this.method = method;
            this.authority = authority;
            this.path = path;
            this.handler = handler;
        }

        @Override
        public AHttpHandler getHandler(final HttpExchange exchange) {
            final URI requestUri = exchange.getRequestUri();
            if (authority != null && !authority.equalsIgnoreCase(requestUri.getAuthority())) {
                return null;
            }
            final String requestPath = requestUri.getPath();
            if (path != null && requestPath != null
                    && !requestPath.equals(path)) {
                return null;
            }
            if (method != null && !method.equals(exchange.getMethod())) {
                return null;
            }
            return handler;
        }
    }
}
