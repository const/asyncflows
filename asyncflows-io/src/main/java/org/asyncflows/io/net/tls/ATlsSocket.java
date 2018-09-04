package org.asyncflows.io.net.tls;

import org.asyncflows.io.net.ASocket;
import org.asyncflows.core.Promise;

import javax.net.ssl.SSLSession;

/**
 * SSL socket interface.
 */
public interface ATlsSocket extends ASocket {
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
