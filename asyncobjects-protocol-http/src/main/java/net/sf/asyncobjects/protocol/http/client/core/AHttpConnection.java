package net.sf.asyncobjects.protocol.http.client.core;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.protocol.http.client.AHttpRequest;

import java.net.SocketAddress;

/**
 * The http connection interface for the specific host.
 */
public interface AHttpConnection extends AStream<AHttpRequest> {
    /**
     * @return the remote address for the socket (hop only)
     */
    Promise<SocketAddress> getRemoteAddress();

    /**
     * @return the remote address for the socket (hop only)
     */
    Promise<SocketAddress> getLocalAddress();
}
