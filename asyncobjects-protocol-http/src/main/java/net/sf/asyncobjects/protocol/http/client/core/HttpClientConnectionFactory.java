package net.sf.asyncobjects.protocol.http.client.core;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketOptions;
import net.sf.asyncobjects.nio.util.ByteGeneratorContext;
import net.sf.asyncobjects.nio.util.ByteParserContext;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.util.AllControl.aAll;

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
        ).unzip(
                (voidValue, input, output) -> aAll(
                        channel::getRemoteAddress
                ).and(
                        channel::getLocalAddress
                ).unzip((remoteAddress, localAddress) -> aValue(new HttpClientConnection(
                                protocol, host,
                                new ByteGeneratorContext(output, bufferSize),
                                new ByteParserContext(input, bufferSize),
                                channel, userAgent, remoteAddress, localAddress).export())
                )
        );
    }
}
