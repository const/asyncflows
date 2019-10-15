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

package org.asyncflows.io.net.tls;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;
import java.util.Objects;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The server socket for TLS.
 */
public class TlsServerSocket extends ChainedClosable<AServerSocket>
        implements ATlsServerSocket, NeedsExport<ATlsServerSocket> {
    /**
     * The engine factory.
     */
    private AFunction<SocketAddress, SSLEngine> engineFactory;
    /**
     * The local address.
     */
    private SocketAddress localAddress;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped       the underlying object
     * @param engineFactory the factory for the SSL engine
     */
    public TlsServerSocket(final AServerSocket wrapped, final AFunction<SocketAddress, SSLEngine> engineFactory) {
        super(wrapped);
        this.engineFactory = engineFactory;
    }


    @Override
    public Promise<Void> setEngineFactory(AFunction<SocketAddress, SSLEngine> engineFactory) {
        Objects.requireNonNull(engineFactory);
        this.engineFactory = engineFactory;
        return aVoid();
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
        return wrapped.bind(address, backlog).flatMap(collectAddress());
    }

    /**
     * This method collects socket address after the bind.
     *
     * @return the void value
     */
    private AFunction<SocketAddress, SocketAddress> collectAddress() {
        return value -> {
            localAddress = value;
            return aValue(value);
        };
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        return wrapped.bind(address).flatMap(collectAddress());
    }

    @Override
    public Promise<Void> setDefaultOptions(final SocketOptions options) {
        return wrapped.setDefaultOptions(options);
    }

    @Override
    public Promise<SocketAddress> getLocalSocketAddress() {
        return wrapped.getLocalSocketAddress();
    }

    @Override
    public Promise<ASocket> accept() {
        return wrapped.accept().flatMap(socket -> engineFactory.apply(localAddress).flatMap(engine -> {
            final TlsSocket sslSocket = new TlsSocket(socket, engineFactory);
            return sslSocket.init(engine).thenValue(sslSocket.export());
        }));
    }

    @Override
    public ATlsServerSocket export(final Vat vat) {
        return ATlsServerSocketProxyFactory.createProxy(vat, this);
    }
}
