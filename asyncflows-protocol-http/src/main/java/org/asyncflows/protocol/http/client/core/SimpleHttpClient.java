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

package org.asyncflows.protocol.http.client.core;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.function.AsyncFunctionUtil.maybeMapper;
import static org.asyncflows.core.streams.AsyncStreams.aForIterable;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.util.ResourceClosedException;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.protocol.http.HttpException;
import org.asyncflows.protocol.http.client.AHttpClient;
import org.asyncflows.protocol.http.client.AHttpClientProxyFactory;
import org.asyncflows.protocol.http.client.AHttpRequest;
import org.asyncflows.protocol.http.client.AHttpRequestProxyFactory;
import org.asyncflows.protocol.http.client.HttpRequestUtil;
import org.asyncflows.protocol.http.client.HttpResponse;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpURIUtil;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The simple HTTP client (no proxies and other things). This client is used mostly for testing.
 */
public class SimpleHttpClient extends CloseableBase implements AHttpClient, ExportableComponent<AHttpClient> {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SimpleHttpClient.class);
    /**
     * The connections.
     */
    private final Map<URI, List<ConnectionWrapper>> connections = new HashMap<>();
    /**
     * The connection factory.
     */
    private final HttpClientConnectionFactory connectionFactory = new HttpClientConnectionFactory();
    /**
     * The timeout to wait until connection is closed.
     */
    private int connectionTimeout = HttpLimits.DEFAULT_IDLE_CONNECTION_TIMEOUT;
    /**
     * The socket factory.
     */
    private ASocketFactory socketFactory;
    /**
     * The connection count.
     */
    private long connectionCount;
    /**
     * The user agent.
     */
    private String userAgent = HttpHeadersUtil.LIBRARY_DESCRIPTION;

    /**
     * Extract key portion from connection URI.
     *
     * @param uri the URI to examine
     * @return the key URI
     */
    @SuppressWarnings("squid:S3398")
    private static URI toKey(final URI uri) {
        try {
            return new URI(uri.getScheme().toLowerCase(), null, uri.getHost().toLowerCase(), HttpURIUtil.getPort(uri),
                    null, null, null);
        } catch (Exception ex) {
            throw new HttpException("BAD URI: " + uri, ex);
        }
    }

    /**
     * @return the user agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Set user agent.
     *
     * @param userAgent the user agent
     */
    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @return the connection timeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set connection timeout.
     *
     * @param connectionTimeout the timeout
     */
    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @return the socket factory
     */
    public ASocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
     * Set socket factory.
     *
     * @param socketFactory the socket factory
     */
    public void setSocketFactory(final ASocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * Peek ready element from the hosts.
     *
     * @param key the connection host to use
     * @return a request instance if there is a ready connection, or null if new connection should be created.
     */
    @SuppressWarnings("squid:S3398")
    private AHttpRequest peekReady(final URI key) {
        ensureOpen();
        cleanup();
        final List<ConnectionWrapper> wrappers = connections.get(key);
        if (wrappers == null) {
            return null;
        }
        for (final ConnectionWrapper wrapper : wrappers) {
            final AHttpRequest r = wrapper.get();
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Cleanup the client.
     */
    private void cleanup() {
        for (final List<ConnectionWrapper> connectionWrappers : connections.values()) {
            for (final ConnectionWrapper wrapper : connectionWrappers) {
                if (wrapper.isExpired(System.currentTimeMillis())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Closing cached connection: %s %s -> %s",
                                wrapper.id, wrapper.connection.getLocalAddress(),
                                wrapper.connection.getRemoteAddress()));
                    }
                    wrapper.close();
                }
            }
        }
    }

    /**
     * Prepare new connection with the specified key.
     *
     * @param key the key to use
     * @return the first request on the prepared connection
     */
    @SuppressWarnings("squid:S3398")
    private Promise<AHttpRequest> connect(final URI key) {
        ensureOpen();
        final Cell<ASocket> socketCell = new Cell<>();
        return aSeq(
                () -> getSocketFactory().makeSocket()
        ).flatMap(socket -> {
            final SocketOptions options = new SocketOptions();
            options.setTpcNoDelay(true);
            if (connectionTimeout > 0) {
                options.setTimeout(connectionTimeout);
            }
            return socket.setOptions(options).thenValue(socket);
        }).flatMap(socket -> {
            socketCell.setValue(socket);
            return socket.connect(new InetSocketAddress(key.getHost(), key.getPort()));
        }).thenFlatGet(() -> {
            // TODO property for buffer size
            return connectionFactory.wrap(key.getAuthority(), key.getScheme(), socketCell.getValue(),
                    HttpLimits.DEFAULT_BUFFER_SIZE, getUserAgent());
        }).flatMap(connection -> {
            ensureOpen();
            final ConnectionWrapper wrapper = new ConnectionWrapper(nextId(), key, connection);
            return wrapper.start();
        }).flatMapFailure(failure -> {
            if (!socketCell.isEmpty()) {
                return socketCell.getValue().close().thenFailure(failure);
            } else {
                return aFailure(failure);
            }
        });
    }

    /**
     * @return the next connection id
     */
    public String nextId() {
        return "httpClient:" + (connectionCount++);
    }

    @Override
    public Promise<AHttpRequest> newRequest() {
        return aValue(new ProxyRequest().export());
    }

    @Override
    protected Promise<Void> closeAction() {
        return aForIterable(new ArrayList<>(connections.values())).all()
                .flatMapIterable(value -> aValue(new ArrayList<>(value))).map(ACloseable::close).toVoid();
    }

    @Override
    public AHttpClient export(Vat vat) {
        return AHttpClientProxyFactory.createProxy(vat, this);
    }


    /**
     * The request that routes the request to the specified host.
     */
    private class ProxyRequest extends CloseableInvalidatingBase implements AHttpRequest, ExportableComponent<AHttpRequest> {
        /**
         * The request.
         */
        private AHttpRequest request;
        /**
         * The resume promise if needed.
         */
        private Promise<Void> requestReadyPromise;


        @Override
        public Promise<SocketAddress> getRemoteAddress() {
            if (request != null) {
                return request.getRemoteAddress();
            } else {
                return requestReady().thenFlatGet(() -> {
                    ensureValidAndOpen();
                    return request.getRemoteAddress();
                });
            }
        }

        @Override
        public Promise<SocketAddress> getLocalAddress() {
            if (request != null) {
                return request.getLocalAddress();
            } else {
                return requestReady().thenFlatGet(() -> {
                    ensureValidAndOpen();
                    return request.getLocalAddress();
                });
            }
        }

        /**
         * @return a promise that resolves when request is ready.
         */
        private Promise<Void> requestReady() {
            if (request != null) {
                return aVoid();
            } else {
                if (requestReadyPromise == null) {
                    requestReadyPromise = new Promise<>();
                }
                return requestReadyPromise;
            }
        }

        @Override
        public Promise<AOutput<ByteBuffer>> request(final Scope scope, final String method, final URI uri,
                                                    final HttpHeaders headersClient, final Long length) {
            try {
                if (!uri.getScheme().equalsIgnoreCase("http")) {
                    // TODO add https later
                    throw new HttpException("Protocol not supported: " + uri.getScheme());
                }
                if (uri.getUserInfo() != null) {
                    throw new HttpException("UserInfo component must be blank, use headers.");
                }
                final HttpHeaders headers = new HttpHeaders(headersClient);
                final String connectionHost = scope.get(HttpRequestUtil.CONNECTION_HOST);
                final URI key;
                if (connectionHost != null) {
                    key = toKey(new URI(uri.getScheme() + "://" + connectionHost));
                } else {
                    key = toKey(uri);
                }
                return aSeqUntilValue(() -> {
                    final AHttpRequest r = peekReady(key);
                    if (r != null) {
                        return r.request(scope, method, uri, headers, length).flatMapOutcome(
                                value -> {
                                    if (value.isSuccess()) {
                                        setRequest(r);
                                        return aMaybeValue(value.value());
                                    } else if (isRetryException(value.failure())) {
                                        return aMaybeEmpty();
                                    } else {
                                        return aFailure(value.failure());
                                    }
                                });
                    } else {
                        return connect(key).flatMap(value -> {
                            setRequest(value);
                            return request.request(scope, method, uri, headers, length).flatMap(
                                    maybeMapper());
                        });
                    }
                }).listen(outcomeChecker());
            } catch (URISyntaxException ex) {
                invalidate(ex);
                return aFailure(ex);
            }
        }

        /**
         * Check if problem causes retry on other connection.
         *
         * @param failure the failure
         * @return true the request should be retryed.
         */
        private boolean isRetryException(final Throwable failure) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Checking if HTTPClient should retry on other connection: %s", failure));
            }
            return !(failure instanceof Error) && !(failure instanceof HttpException);
        }

        /**
         * Set the request to the proxy.
         *
         * @param request the request.
         */
        private void setRequest(final AHttpRequest request) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Request ready: %s", request));
            }
            this.request = request;
            if (requestReadyPromise != null) {
                notifySuccess(requestReadyPromise.resolver(), null);
                requestReadyPromise = null;
            }
        }


        @Override
        public Promise<HttpResponse> getResponse() {
            try {
                ensureValidAndOpen();
            } catch (Throwable throwable) {
                return aFailure(throwable);
            }
            if (request != null) {
                return request.getResponse().listen(outcomeChecker());
            } else {
                return requestReady().thenFlatGet(() -> {
                    ensureValidAndOpen();
                    return request.getResponse().listen(outcomeChecker());
                });
            }
        }

        /**
         * The invalidation callback.
         *
         * @param throwable the invalidation reason
         */
        @Override
        protected void onInvalidation(final Throwable throwable) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Client invalidated", throwable);
            }
            if (requestReadyPromise != null) {
                notifyFailure(requestReadyPromise.resolver(), throwable);
                requestReadyPromise = null;
            }
            super.onInvalidation(throwable);
        }

        @Override
        protected Promise<Void> closeAction() {
            if (requestReadyPromise != null) {
                notifyFailure(requestReadyPromise.resolver(), new ResourceClosedException("Request closed"));
            }
            if (request != null) {
                return request.close();
            } else {
                return aVoid();
            }
        }

        @Override
        public AHttpRequest export() {
            return export(Vat.current());
        }

        @Override
        public AHttpRequest export(final Vat vat) {
            return AHttpRequestProxyFactory.createProxy(vat, this);
        }
    }

    /**
     * The wrapper for the connection that keeps some control information and allows controlling the connection.
     */
    private final class ConnectionWrapper extends CloseableBase {
        /**
         * The wrapper id.
         */
        private final String id;
        /**
         * The request queue.
         */
        private final RequestQueue requests = new RequestQueue();
        /**
         * The host for connection.
         */
        private final URI key;
        /**
         * The HTTP connection.
         */
        private final AHttpConnection connection;
        /**
         * The next request.
         */
        private AHttpRequest nextRequest;
        /**
         * The time the when the last result has been got.
         */
        private long idleSince;

        /**
         * The wrapper.
         *
         * @param id         the connection id
         * @param key        the key component of connection URI (only protocol, host and port)
         * @param connection the connection
         */
        private ConnectionWrapper(final String id, final URI key, final AHttpConnection connection) {
            this.id = id;
            this.key = key;
            this.connection = connection;
        }


        /**
         * @return start working with wrapper.
         */
        @SuppressWarnings("java:S5411")
        public Promise<AHttpRequest> start() {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Starting connection %s to %s", id, key));
            }
            return nextRequest().flatMap(value -> {
                if (!value) {
                    throw new HttpException("Connection is broken!");
                }
                final AHttpRequest request = nextRequest;
                nextRequest = null;
                run();
                return aValue(request);
            });
        }

        /**
         * @return http request if it is ready
         */
        public AHttpRequest get() {
            final AHttpRequest request = nextRequest;
            if (request != null) {
                nextRequest = null;
                requests.resume();
            }
            return request;
        }

        /**
         * run acquire loop.
         */
        @SuppressWarnings("squid:S3776")
        private void run() {
            List<ConnectionWrapper> connectionList = connections.computeIfAbsent(key, k -> new LinkedList<>());
            connectionList.add(this);
            aSeq(() -> requests.runSeqWhile(
                    () -> requests.suspend().thenFlatGet(() -> {
                        if (!isOpen()) {
                            return aFalse();
                        }
                        if (nextRequest != null) {
                            return aTrue();
                        } else {
                            return nextRequest();
                        }

                    }))
            ).finallyDo(this::close).listen(resolution -> {
                if (resolution.isSuccess()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Connection %s to %s finished", id, key));
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Connection %s to %s failed", id, key), resolution.failure());
                    }
                }
            });
        }

        /**
         * @return get the next request
         */
        private Promise<Boolean> nextRequest() {
            return connection.next().flatMap(value -> {
                if (value.isEmpty()) {
                    return aFalse();
                } else {
                    nextRequest = value.of();
                    idleSince = System.currentTimeMillis();
                    return aTrue();
                }
            });
        }


        @Override
        protected Promise<Void> closeAction() {
            final List<ConnectionWrapper> connectionWrappers = connections.get(key);
            connectionWrappers.remove(this);
            if (connectionWrappers.isEmpty()) {
                connections.remove(key);
            }
            requests.resume();
            return connection.close();
        }

        /**
         * Check if connection is expired.
         *
         * @param time the time
         * @return the connection time
         */
        public boolean isExpired(final long time) {
            return nextRequest != null && time - idleSince > getConnectionTimeout();
        }
    }
}
