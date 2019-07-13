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

package org.asyncflows.io.net.async;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.adapters.nio.AsyncNioFlows;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.AServerSocketProxyFactory;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

class AsyncServerSocket extends CloseableInvalidatingBase implements AServerSocket, NeedsExport<AServerSocket> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncServerSocket.class);
    private final AsynchronousServerSocketChannel channel;
    private SocketOptions defaultOptions;

    AsyncServerSocket(AsynchronousServerSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public AServerSocket export(Vat vat) {
        return AServerSocketProxyFactory.createProxy(vat, this);
    }

    @Override
    public Promise<SocketAddress> bind(SocketAddress address, int backlog) {
        return aNow(() -> {
            channel.bind(address, backlog);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Bound to %s", channel.getLocalAddress()));
            }
            return aValue(channel.getLocalAddress());
        });
    }

    @Override
    public Promise<SocketAddress> bind(SocketAddress address) {
        return aNow(() -> {
            channel.bind(address);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Bound to %s", channel.getLocalAddress()));
            }
            return aValue(channel.getLocalAddress());
        });
    }

    @Override
    public Promise<Void> setDefaultOptions(SocketOptions options) {
        defaultOptions = options;
        return aVoid();
    }

    @Override
    public Promise<SocketAddress> getLocalSocketAddress() {
        return aNow(() -> aValue(channel.getLocalAddress()));
    }

    @Override
    public Promise<ASocket> accept() {
        return AsyncNioFlows.<AsynchronousSocketChannel>aCompletionHandler(channel::accept).flatMap(c -> {
            final AsyncSocket asyncSocket = new AsyncSocket(c);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Connection established %s", asyncSocket.channelId()));
            }
            if (defaultOptions != null) {
                asyncSocket.setOptions(defaultOptions);
            }
            return aValue(asyncSocket.export());
        });
    }
}
