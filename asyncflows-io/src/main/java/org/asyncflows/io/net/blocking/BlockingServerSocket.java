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

package org.asyncflows.io.net.blocking;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.SocketUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The blocking server socket implementation. Note that by default it exports on the daemon vat.
 */
public class BlockingServerSocket extends CloseableInvalidatingBase
        implements AServerSocket, ExportableComponent<AServerSocket> {
    /**
     * The wrapped socket.
     */
    private final ServerSocket serverSocket;
    /**
     * The socket options.
     */
    private SocketOptions options;

    /**
     * The constructor.
     *
     * @throws IOException if there is a problem with creating socket
     */
    public BlockingServerSocket() throws IOException {
        serverSocket = new ServerSocket();
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
        try {
            serverSocket.bind(address, backlog);
            return getLocalSocketAddress();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        try {
            serverSocket.bind(address);
            return getLocalSocketAddress();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> setDefaultOptions(final SocketOptions newOptions) {
        if (newOptions != null) {
            options = newOptions.copy();
        } else {
            options = null;
        }
        return aVoid();
    }

    @Override
    public Promise<SocketAddress> getLocalSocketAddress() {
        try {
            return aValue(serverSocket.getLocalSocketAddress());
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    @SuppressWarnings("squid:S1141")
    public Promise<ASocket> accept() {
        try {
            final Socket accepted = serverSocket.accept();
            try {
                if (options != null) {
                    SocketUtil.applyOptions(accepted, options);
                }
            } catch (Throwable t) {
                accepted.close();
                return aFailure(t);
            }
            return aValue(new BlockingSocket(accepted).export());
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            serverSocket.close();
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public AServerSocket export() {
        return BlockingSocketExportUtil.export(Vats.daemonVat(), Vats.daemonVat(), this);
    }

    @Override
    public AServerSocket export(final Vat vat) {
        return export();
    }
}
