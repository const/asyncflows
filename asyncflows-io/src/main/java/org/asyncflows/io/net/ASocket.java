/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.io.net;


import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.io.AChannel;
import org.asyncflows.core.Promise;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * The basic socket interface.
 */
@Asynchronous
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
