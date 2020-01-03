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

package org.asyncflows.protocol.http.client.core;

import org.asyncflows.core.Promise;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;

/**
 * The connection factory.
 */
public class HttpClientConnectionFactory {

    /**
     * Create an connection over the specified channel.
     *
     * @param host       the connection host
     * @param protocol   the protocol
     * @param channel    the channel
     * @param bufferSize the size of buffer to use
     * @param userAgent  the user agent
     * @return the connection
     */
    public Promise<AHttpConnection> wrap(final String host, final String protocol,
                                         final ASocket channel, final int bufferSize,
                                         final String userAgent) {
        final SocketOptions socketOptions = new SocketOptions();
        socketOptions.setTpcNoDelay(true);
        return aAll(
                () -> channel.setOptions(socketOptions)
        ).and(
                channel::getInput
        ).and(
                channel::getOutput
        ).map(
                (voidValue, input, output) -> aAll(
                        channel::getRemoteAddress
                ).and(
                        channel::getLocalAddress
                ).map((remoteAddress, localAddress) -> aValue(new HttpClientConnection(
                        protocol, host,
                        new ByteGeneratorContext(output, bufferSize),
                        new ByteParserContext(input, bufferSize),
                        channel, userAgent, remoteAddress, localAddress).export())
                )
        );
    }
}
