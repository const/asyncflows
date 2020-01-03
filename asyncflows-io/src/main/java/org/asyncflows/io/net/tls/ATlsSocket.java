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

package org.asyncflows.io.net.tls;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.io.net.ASocket;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.net.SocketAddress;

/**
 * SSL socket interface.
 */
@Asynchronous
public interface ATlsSocket extends ASocket {
    /**
     * Set engine factory for the connection.
     *
     * @param engineFactory the engine factory
     * @return when operation complete
     */
    Promise<Void> setEngineFactory(AFunction<SocketAddress, SSLEngine> engineFactory);

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
