package net.sf.asyncobjects.nio.net.ssl;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.net.ASocket;

import javax.net.ssl.SSLSession;

/**
 * SSL socket interface.
 */
public interface ASSLSocket extends ASocket {
    /**
     * Starts a handshake if it is not in progress. If handshake is in progress, just returns a promise that resolves
     * when handshake is finished.
     *
     * @return a promise for the handshake result
     */
    Promise<Void> handshake();

    /**
     * @return the SSL session
     */
    Promise<SSLSession> getSession();
}
