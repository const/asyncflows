package net.sf.asyncobjects.nio.net;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.AChannel;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * The basic socket interface.
 */
public interface ASocket extends AChannel<ByteBuffer> {
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
     * @return the remote address for the socket
     */
    Promise<SocketAddress> getRemoteAddress();

    /**
     * @return the remote address for the socket
     */
    Promise<SocketAddress> getLocalAddress();
}
