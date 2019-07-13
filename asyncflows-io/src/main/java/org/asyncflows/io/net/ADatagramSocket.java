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

package org.asyncflows.io.net;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.ACloseable;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * The datagram socket.
 */
@Asynchronous
public interface ADatagramSocket extends ACloseable {
    /**
     * Set socket options.
     *
     * @param options the options to set
     * @return when options are set
     */
    Promise<Void> setOptions(SocketOptions options);

    /**
     * Connect to the remote address.
     *
     * @param address the address to connect to
     * @return when connected
     */
    Promise<Void> connect(SocketAddress address);

    /**
     * Disconnect from the remote address.
     *
     * @return when disconnected
     */
    Promise<Void> disconnect();

    /**
     * @return the remote address for the socket
     */
    Promise<SocketAddress> getRemoteAddress();

    /**
     * @return the local address for the socket
     */
    Promise<SocketAddress> getLocalAddress();

    /**
     * Bind to specified port an host.
     *
     * @param address the socket address
     * @return when operation finishes return local socket address
     * @see java.net.ServerSocket#bind(java.net.SocketAddress, int)
     */
    Promise<SocketAddress> bind(SocketAddress address);

    /**
     * Send data to socket to which this datagram socket is connected.
     *
     * @param buffer the buffer to send to
     * @return a promise indicating that data was sent
     */
    Promise<Void> send(ByteBuffer buffer);

    /**
     * Send data to the specified address.
     *
     * @param address the address to send to
     * @param buffer  the buffer to send to
     * @return a promise indicating that data was sent
     */
    Promise<Void> send(SocketAddress address, ByteBuffer buffer);

    /**
     * Receive a datagram from the specified address.
     *
     * @param buffer the buffer.
     * @return the address from which datagram is received.
     */
    Promise<SocketAddress> receive(ByteBuffer buffer);
}
