package net.sf.asyncobjects.protocol.http.core.handlers;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.AConsumer;
import net.sf.asyncobjects.core.util.LogUtil;
import net.sf.asyncobjects.core.util.ResourceClosedException;
import net.sf.asyncobjects.nio.AChannel;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.IOUtil;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.protocol.http.HttpStatusException;
import net.sf.asyncobjects.protocol.http.common.HttpLimits;
import net.sf.asyncobjects.protocol.http.common.HttpMethodUtil;
import net.sf.asyncobjects.protocol.http.common.HttpStatusUtil;
import net.sf.asyncobjects.protocol.http.common.content.CountingInput;
import net.sf.asyncobjects.protocol.http.common.content.CountingOutput;
import net.sf.asyncobjects.protocol.http.common.content.StreamFinishedEvent;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeaders;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.HttpHandlerBase;
import net.sf.asyncobjects.protocol.http.server.core.ExchangeFinishedEvent;
import net.sf.asyncobjects.protocol.http.server.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResourceAction;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.nio.net.SocketUtil.aTrySocket;

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
                    return ResponseUtil.discardAndClose(exchange.getInput()).thenDo(
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
        return new CountingInput<ByteBuffer>(stream, new AConsumer<StreamFinishedEvent>() {
            @Override
            public void accept(final StreamFinishedEvent event) {
                exchange.getExchangeScope().set(ExchangeFinishedEvent.REMOTE_TO_SERVER, event);
            }
        });
    }

    /**
     * Count bytes on output.
     *
     * @param exchange the exchange to report to
     * @param stream   the stream to count
     * @return proxy stream
     */
    private AOutput<ByteBuffer> count(final HttpExchange exchange, final AOutput<ByteBuffer> stream) {
        return new CountingOutput<ByteBuffer>(stream, new AConsumer<StreamFinishedEvent>() {
            @Override
            public void accept(final StreamFinishedEvent event) {
                exchange.getExchangeScope().set(ExchangeFinishedEvent.SERVER_TO_REMOTE, event);
            }
        });
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
    private ACallable<Void> copyAndClose(final AChannel<ByteBuffer> socket,
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
                ).toVoid()).observe(LogUtil.checkpoint(LOG, () -> "Copy finished " + direction));
    }
}
