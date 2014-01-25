package net.sf.asyncobjects.nio.net;

import net.sf.asyncobjects.core.Promise;

/**
 * Socket factory.
 */
public interface ASocketFactory {
    /**
     * @return the promise for a plain socket
     */
    Promise<ASocket> makeSocket();

    /**
     * @return the promise for server socket
     */
    Promise<AServerSocket> makeServerSocket();

    /**
     * @return the promise for datagram socket
     */
    Promise<ADatagramSocket> makeDatagramSocket();
}
