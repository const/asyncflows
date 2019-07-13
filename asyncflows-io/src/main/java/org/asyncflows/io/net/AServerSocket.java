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

package org.asyncflows.io.net;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.ACloseable;

import java.net.SocketAddress;

/**
 * A server socket.
 */
@Asynchronous
public interface AServerSocket extends ACloseable {
    /**
     * Bind to specified port and host.
     *
     * @param address the socket address
     * @param backlog a backlog of socket
     * @return when operation finishes return local socket address
     * @see java.net.ServerSocket#bind(java.net.SocketAddress, int)
     */
    Promise<SocketAddress> bind(SocketAddress address, int backlog);

    /**
     * Bind to specified port an host.
     *
     * @param address the socket address
     * @return when operation finishes return local socket address
     * @see java.net.ServerSocket#bind(java.net.SocketAddress, int)
     */
    Promise<SocketAddress> bind(SocketAddress address);

    /**
     * Set options that are automatically applied for each incoming connection.
     *
     * @param options the options to set (the object is copied)
     * @return when defaults are set
     */
    Promise<Void> setDefaultOptions(SocketOptions options);

    /**
     * @return the promise for local socket address.
     */
    Promise<SocketAddress> getLocalSocketAddress();

    /**
     * Accept incoming connection.
     *
     * @return a socket when connection is received
     * @see java.net.ServerSocket#accept()
     */
    Promise<ASocket> accept();
}
