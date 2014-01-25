package net.sf.asyncobjects.nio.net;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * The datagram socket.
 */
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
