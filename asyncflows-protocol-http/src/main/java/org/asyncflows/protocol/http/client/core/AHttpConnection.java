package org.asyncflows.protocol.http.client.core;

import org.asyncflows.protocol.http.client.AHttpRequest;
import org.asyncflows.core.Promise;
import org.asyncflows.core.streams.AStream;

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
