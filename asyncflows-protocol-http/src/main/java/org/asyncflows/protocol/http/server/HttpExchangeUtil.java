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

package org.asyncflows.protocol.http.server;

import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.content.StreamFinishedEvent;

import java.net.SocketAddress;

/**
 * Utilities to support implementation of handlers.
 */
public final class HttpExchangeUtil {
    /**
     * The key on server scope that allows getting the server address.
     */
    public static final Scope.Key<SocketAddress> SERVER_ADDRESS = new Scope.Key<>(HttpExchange.class, "serverAddress");
    /**
     * Key used to report remote address.
     */
    public static final Scope.Key<String> REMOTE = new Scope.Key<>(HttpExchange.class, "remote");
    /**
     * The key used by a handler to report bytes transferred to remote server in a case of proxy or connect functionality.
     */
    public static final Scope.Key<StreamFinishedEvent> REMOTE_TO_SERVER
            = new Scope.Key<>(HttpExchange.class, "remoteToServer");
    /**
     * The key used by a handler to report bytes transferred from remote server a case of proxy or connect functionality.
     */
    public static final Scope.Key<StreamFinishedEvent> SERVER_TO_REMOTE
            = new Scope.Key<>(HttpExchange.class, "serverToRemote");

    private HttpExchangeUtil() {
    }
}
