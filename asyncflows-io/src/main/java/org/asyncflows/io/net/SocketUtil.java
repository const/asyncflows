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

package org.asyncflows.io.net;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CoreFlowsResource;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOUtil;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;

/**
 * Socket utilities.
 */
public final class SocketUtil {
    /**
     * The private constructor for utility class.
     */
    private SocketUtil() {
    }

    /**
     * Apply socket options to the {@link java.net.Socket}.
     *
     * @param socket  the socket
     * @param options the options.
     * @throws SocketException if the option could not be set
     */
    public static void applyOptions(final Socket socket, final SocketOptions options) throws SocketException {
        final Boolean tpcNoDelay = options.getTpcNoDelay();
        if (tpcNoDelay != null) {
            socket.setTcpNoDelay(tpcNoDelay);
        }
        final Boolean keepAlive = options.getKeepAlive();
        if (keepAlive != null) {
            socket.setKeepAlive(keepAlive);
        }
        final Integer linger = options.getLinger();
        if (linger != null) {
            socket.setSoLinger(linger >= 0, linger);
        }
        final Boolean oobInline = options.getOobInline();
        if (oobInline != null) {
            socket.setOOBInline(oobInline);
        }
        final Integer receiveBufferSize = options.getReceiveBufferSize();
        if (receiveBufferSize != null) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }
        final Integer sendBufferSize = options.getSendBufferSize();
        if (sendBufferSize != null) {
            socket.setSendBufferSize(sendBufferSize);
        }
        final Integer timeout = options.getTimeout();
        if (timeout != null) {
            socket.setSoTimeout(timeout);
        }
        final Integer trafficClass = options.getTrafficClass();
        if (trafficClass != null) {
            socket.setTrafficClass(trafficClass);
        }
    }

    /**
     * Apply socket options to the {@link java.net.DatagramSocket}.
     *
     * @param socket  the socket
     * @param options the options.
     * @throws SocketException if the option could not be set
     */
    public static void applyOptions(final DatagramSocket socket,
                                    final SocketOptions options) throws SocketException {
        final Integer receiveBufferSize = options.getReceiveBufferSize();
        if (receiveBufferSize != null) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }
        final Integer sendBufferSize = options.getSendBufferSize();
        if (sendBufferSize != null) {
            socket.setSendBufferSize(sendBufferSize);
        }
        final Integer timeout = options.getTimeout();
        if (timeout != null) {
            socket.setSoTimeout(timeout);
        }
        final Integer trafficClass = options.getTrafficClass();
        if (trafficClass != null) {
            socket.setTrafficClass(trafficClass);
        }
        final Boolean broadcast = options.getBroadcast();
        if (broadcast != null) {
            socket.setBroadcast(broadcast);
        }
        final Boolean reuseAddress = options.getReuseAddress();
        if (reuseAddress != null) {
            socket.setReuseAddress(reuseAddress);
        }
    }

    /**
     * Try channel.
     *
     * @param socket a socket
     * @return a try operation on the socket
     */
    public static CoreFlowsResource.Try3<ASocket, AInput<ByteBuffer>, AOutput<ByteBuffer>> aTrySocket(
            final ASupplier<ASocket> socket) {
        return IOUtil.aTryChannel(socket);
    }

    /**
     * Try channel.
     *
     * @param socket a socket
     * @return a try operation on the socket
     */
    public static CoreFlowsResource.Try3<ASocket, AInput<ByteBuffer>, AOutput<ByteBuffer>> aTrySocket(
            final Promise<ASocket> socket) {
        return aTrySocket(promiseSupplier(socket));
    }

    /**
     * Create a server socket on free port.
     *
     * @param factory the socket factory to use
     * @return pair of the created socket and port it allocates
     */
    public static Promise<Tuple2<AServerSocket, SocketAddress>> anonymousServerSocket(final ASocketFactory factory) {
        return factory.makeServerSocket().flatMap(
                serverSocket -> serverSocket.bind(new InetSocketAddress(0)).flatMap(
                        socketAddress -> aValue(Tuple2.of(serverSocket, socketAddress))));
    }
}
