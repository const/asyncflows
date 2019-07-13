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
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;

/**
 * The SSL socket.
 */
public class TlsSocket extends TlsChannel<ASocket> implements ATlsSocket, NeedsExport<ATlsSocket> {
    /**
     * The engine factory.
     */
    private final AFunction<SocketAddress, SSLEngine> engineFactory;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped       the underlying object
     * @param engineFactory the {@link SSLEngine} factory.
     */
    public TlsSocket(final ASocket wrapped, final AFunction<SocketAddress, SSLEngine> engineFactory) {
        super(wrapped);
        this.engineFactory = engineFactory;
    }

    @Override
    public Promise<Void> handshake() {
        return doHandshake();
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        return wrapped.setOptions(options);
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        if (getEngine() != null) {
            throw new IllegalStateException("SSLEngine is already initialized!");
        }
        return wrapped.connect(address).thenFlatGet(() -> engineFactory.apply(address).flatMap(this::init));
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return wrapped.getRemoteAddress();
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return wrapped.getLocalAddress();
    }

    @Override
    public ATlsSocket export(final Vat vat) {
        return ATlsSocketProxyFactory.createProxy(vat, this);
    }
}
