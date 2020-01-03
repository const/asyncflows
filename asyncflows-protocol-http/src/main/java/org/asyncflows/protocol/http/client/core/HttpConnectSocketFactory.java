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

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.ASocketFactoryProxyFactory;
import org.asyncflows.io.net.ASocketProxyFactory;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.protocol.http.client.AHttpClient;
import org.asyncflows.protocol.http.client.AHttpRequest;
import org.asyncflows.protocol.http.client.HttpRequestUtil;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.HttpURIUtil;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;

/**
 * The socket factory.
 */
public class HttpConnectSocketFactory implements ASocketFactory, NeedsExport<ASocketFactory> {
    /**
     * The http client (note that it should be closed, after the factory is not used anymore).
     */
    private final AHttpClient client;
    /**
     * The proxy host.
     */
    private final String proxyHost;

    /**
     * The constructor.
     *
     * @param client    the client to use
     * @param proxyHost the proxy host
     */
    public HttpConnectSocketFactory(final AHttpClient client, final String proxyHost) {
        this.client = client;
        this.proxyHost = proxyHost;
    }

    /**
     * @return the promise for a plain socket
     */
    @Override
    public Promise<ASocket> makeSocket() {
        return aValue(new ConnectSocket().export());
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        throw new UnsupportedOperationException("The operation is not supported");
    }

    @Override
    public Promise<ADatagramSocket> makeDatagramSocket() {
        throw new UnsupportedOperationException("The operation is not supported");
    }

    @Override
    public ASocketFactory export(final Vat vat) {
        return ASocketFactoryProxyFactory.createProxy(vat, this);
    }


    /**
     * The connect operation.
     */
    private class ConnectSocket extends CloseableInvalidatingBase implements ASocket, NeedsExport<ASocket> {
        /**
         * The connect scope.
         */
        private final Scope scope = new Scope();
        /**
         * The http request to use.
         */
        private AHttpRequest httpRequest;
        /**
         * The input for the socket.
         */
        private AInput<ByteBuffer> input;
        /**
         * The output for the socket.
         */
        private AOutput<ByteBuffer> output;
        /**
         * The remote address.
         */
        private SocketAddress remoteAddress;

        @Override
        public Promise<Void> setOptions(final SocketOptions options) {
            // ignore this method.
            return aVoid();
        }

        @Override
        public Promise<Void> connect(final SocketAddress address) {
            if (address == null) {
                return aFailure(new ConnectException("Address must not be null"));
            }
            if (remoteAddress != null) {
                return aFailure(new ConnectException("Connect could be called only once"));
            }
            remoteAddress = address;
            return aSeq(client::newRequest).map(request -> {
                httpRequest = request;
                scope.set(HttpRequestUtil.CONNECTION_HOST, proxyHost);
                return httpRequest.request(scope,
                        HttpMethodUtil.CONNECT,
                        new URI("http://" + HttpURIUtil.getHost(address)),
                        new HttpHeaders(),
                        HttpRequestUtil.NO_CONTENT);
            }).map(ACloseable::close).thenDo(() -> {
                // TODO Http client and server options (NO_WAIT)
                // TODO get local address from the scope
                return httpRequest.getResponse();
            }).map(response -> {
                if (!HttpStatusUtil.isSuccess(response.getStatusCode())) {
                    throw new ConnectException("Unable to execute request: "
                            + response.getStatusCode() + " " + response.getReason());
                }
                if (response.getSwitchedChannel() == null) {
                    throw new ConnectException("No switch protocol happened.");
                }
                return aAll(
                        () -> response.getSwitchedChannel().getInput()
                ).and(
                        () -> response.getSwitchedChannel().getOutput()
                ).map((channelInput, channelOutput) -> {
                    input = channelInput;
                    output = channelOutput;
                    return aVoid();
                });
            }).failedLast(value -> {
                if (value instanceof SocketException) {
                    return aFailure(value);
                }
                final ConnectException exception = new ConnectException("Failed to connect: " + value.getMessage());
                exception.initCause(value);
                return aFailure(exception);
            });
        }

        @Override
        public Promise<SocketAddress> getRemoteAddress() {
            if (remoteAddress == null) {
                return socketNotConnected();
            }
            return aValue(remoteAddress);
        }

        private <T> Promise<T> socketNotConnected() {
            return aFailure(new SocketException("Socket not connected"));
        }

        @Override
        public Promise<SocketAddress> getLocalAddress() {
            if (httpRequest == null) {
                return socketNotConnected();
            }
            return httpRequest.getLocalAddress();
        }

        @Override
        public Promise<AInput<ByteBuffer>> getInput() {
            if (input == null) {
                return socketNotConnected();
            }
            return aValue(input);
        }

        @Override
        public Promise<AOutput<ByteBuffer>> getOutput() {
            if (output == null) {
                return socketNotConnected();
            }
            return aValue(output);
        }

        @Override
        protected Promise<Void> closeAction() {
            if (httpRequest != null) {
                return httpRequest.close();
            } else {
                return aVoid();
            }
        }

        @Override
        public ASocket export(final Vat vat) {
            return ASocketProxyFactory.createProxy(vat, this);
        }
    }
}
