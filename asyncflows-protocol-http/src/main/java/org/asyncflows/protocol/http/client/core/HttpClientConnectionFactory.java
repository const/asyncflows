package org.asyncflows.protocol.http.client.core;

import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.core.Promise;

import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.util.AsyncAllControl.aAll;

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
