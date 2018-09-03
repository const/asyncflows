package org.asyncflows.io.net;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import java.net.SocketAddress;

/**
 * A server socket.
 */
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
