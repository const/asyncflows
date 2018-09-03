package org.asyncflows.protocol.http.core.handlers;

import org.asyncflows.io.AChannel;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.protocol.http.HttpStatusException;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.HttpMethodUtil;
import org.asyncflows.protocol.http.common.HttpStatusUtil;
import org.asyncflows.protocol.http.common.content.CountingInput;
import org.asyncflows.protocol.http.common.content.CountingOutput;
import org.asyncflows.protocol.http.common.headers.HttpHeaders;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.HttpHandlerBase;
import org.asyncflows.protocol.http.server.core.ExchangeFinishedEvent;
import org.asyncflows.protocol.http.server.util.ResponseUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.LogUtil;
import org.asyncflows.core.util.ResourceClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import static org.asyncflows.io.net.SocketUtil.aTrySocket;
import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aVoid;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.asyncflows.core.util.AsyncSeqControl.aSeq;
import static org.asyncflows.core.util.ResourceUtil.closeResourceAction;

/**
 * Simple connect handler that implements switch protocol action.
 */
public class ConnectHandler extends HttpHandlerBase {
    // TODO localhost checks for connect (only localhost could connect to this handler)
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConnectHandler.class);
    /**
     * The socket factory.
     */
    private final ASocketFactory socketFactory;

    /**
     * The constructor.
     *
     * @param socketFactory the socket factory
     */
    public ConnectHandler(final ASocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public Promise<Void> handle(final HttpExchange exchange) {
        if (!HttpMethodUtil.isConnect(exchange.getMethod())) {
            throw new HttpStatusException(HttpStatusUtil.INTERNAL_SERVER_ERROR, "The CONNECT method is expected.");
        }
        // For real handler there should be some security implemented. This one is just for the test.
        // Also there should be timeouts.
        final URI uri = exchange.getRequestUri();
        final InetSocketAddress address = new InetSocketAddress(uri.getHost(), uri.getPort());
        return aTrySocket(socketFactory.makeSocket()).run(
                (socket, socketInput, socketOutput) -> aSeq(
                        () -> socket.connect(address)
                ).failed(exception -> {
                    throw new HttpStatusException(HttpStatusUtil.BAD_GATEWAY,
                            "Unable to connect", exception);
                }).thenDo(() -> {
                    exchange.getExchangeScope().set(ExchangeFinishedEvent.REMOTE, uri.getAuthority());
                    return ResponseUtil.discardAndClose(exchange.getInput()).thenFlatGet(
                            closeResourceAction(exchange.getInput()));
                }).thenDo(() -> IOUtil.BYTE.tryChannel(
                        exchange.switchProtocol(HttpStatusUtil.OK, null, new HttpHeaders())).run(
                        (connection, input, output) -> aAll(
                                copyAndClose(socket, connection,
                                        count(exchange, socketInput), output, "serverToClient")
                        ).andLast(
                                copyAndClose(socket, connection,
                                        input, count(exchange, socketOutput), "clientToServer")
                        ).toVoid()
                )).failedLast(exception -> {
                    if (exception instanceof ResourceClosedException) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Connection is closed, possibly handler shutdown", exception);
                        }
                        return aVoid();
                    } else {
                        return aFailure(exception);
                    }
                }));
    }

    /**
     * Count bytes on input.
     *
     * @param exchange the exchange to report to
     * @param stream   the stream to count
     * @return proxy stream
     */
    private AInput<ByteBuffer> count(final HttpExchange exchange, final AInput<ByteBuffer> stream) {
        return new CountingInput<>(stream,
                event -> exchange.getExchangeScope().set(ExchangeFinishedEvent.REMOTE_TO_SERVER, event));
    }

    /**
     * Count bytes on output.
     *
     * @param exchange the exchange to report to
     * @param stream   the stream to count
     * @return proxy stream
     */
    private AOutput<ByteBuffer> count(final HttpExchange exchange, final AOutput<ByteBuffer> stream) {
        return new CountingOutput<ByteBuffer>(stream,
                event -> exchange.getExchangeScope().set(ExchangeFinishedEvent.SERVER_TO_REMOTE, event));
    }


    /**
     * If copy is successful, gracefully closes a connection, otherwise force closes channels.
     *
     * @param socket     the socket connection
     * @param connection the connection
     * @param input      the input
     * @param output     the output
     * @param direction  the direction
     * @return when copy in one direction finishes.
     */
    private ASupplier<Void> copyAndClose(final AChannel<ByteBuffer> socket,
                                         final AChannel<ByteBuffer> connection,
                                         final AInput<ByteBuffer> input,
                                         final AOutput<ByteBuffer> output,
                                         final String direction) {
        return () -> aSeq(() -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Copy started: " + direction);
            }
            return IOUtil.BYTE.copy(input, output, false,
                    ByteBuffer.allocate(HttpLimits.DEFAULT_BUFFER_SIZE)).toVoid();
        }).failed(exception -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Copy failed: " + direction, exception);
            }
            return aAll(closeResourceAction(socket))
                    .andLast(closeResourceAction(connection)).thenFailure(exception);
        }).finallyDo(
                () -> aAll(
                        closeResourceAction(input)
                ).andLast(
                        closeResourceAction(output)
                ).toVoid()).listen(LogUtil.checkpoint(LOG, () -> "Copy finished " + direction));
    }
}
