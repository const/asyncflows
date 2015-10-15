package net.sf.asyncobjects.protocol.http.server.core;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.stream.Streams;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.util.LogUtil;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.protocol.http.common.HttpLimits;
import net.sf.asyncobjects.protocol.http.common.Scope;
import net.sf.asyncobjects.protocol.http.common.headers.HttpHeadersUtil;
import net.sf.asyncobjects.protocol.http.server.AHttpHandler;
import net.sf.asyncobjects.protocol.http.server.HttpExchange;
import net.sf.asyncobjects.protocol.http.server.util.DelegatingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResourceAction;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;

/**
 * The basic HTTP 1.1 server. Note, that it uses only the single normal request handler. Additional handlers might be
 * registered using delegation.
 */
public class HttpServer extends CloseableBase {
    /**
     * The problem with the request.
     */
    public static final Scope.Key<Throwable> BAD_REQUEST_PROBLEM
            = new Scope.Key<>(HttpServer.class, "badRequestProblem");
    /**
     * The key if set to exchange scope prevents writing reply message in the case of switching protocol
     * (needed for HTTP 2.0).
     */
    public static final Scope.Key<Boolean> SWITCH_NO_REPLY
            = new Scope.Key<>(HttpServer.class, "switchNoReply", false);
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);
    /**
     * The server scope.
     */
    private final Scope serverScope = new Scope();
    /**
     * The connections.
     */
    private final Set<HttpServerConnection> connections = new LinkedHashSet<>();
    /**
     * The size of HTTP buffer.
     */
    private int httpBufferSize = HttpLimits.DEFAULT_BUFFER_SIZE;
    /**
     * The handler for the requests. It should be set up before server is started.
     */
    private AServerSocket socket;
    /**
     * The handler for the requests.
     */
    private AHttpHandler handler = new DelegatingHandler();
    /**
     * The handler for the bad requests.
     */
    private AHttpHandler badRequestHandler = new BadRequestHandler();
    /**
     * The library description.
     */
    private String serverDescription = HttpHeadersUtil.LIBRARY_DESCRIPTION;
    /**
     * Amount of accepted connections.
     */
    private long connectionsCount;

    /**
     * @return the server scope
     */
    public Scope getServerScope() {
        return serverScope;
    }

    /**
     * @return the buffer size for HTTp protocol.
     */
    public int getHttpBufferSize() {
        return httpBufferSize;
    }

    /**
     * Set the buffer size.
     *
     * @param httpBufferSize the buffer size.
     */
    public void setHttpBufferSize(final int httpBufferSize) {
        this.httpBufferSize = httpBufferSize;
    }

    /**
     * Set the socket.
     *
     * @param socket the socket.
     */
    public void setSocket(final AServerSocket socket) {
        this.socket = socket;
    }

    /**
     * @return the server description string.
     */
    public String getServerDescription() {
        return serverDescription;
    }

    /**
     * Set server description string.
     *
     * @param serverDescription the description
     */
    public void setServerDescription(final String serverDescription) {
        this.serverDescription = serverDescription;
    }

    /**
     * Run the server.
     *
     * @return the server accept->handle loop.
     */
    public Promise<Void> run() {
        return socket.getLocalSocketAddress().map(socketAddress -> {
            getServerScope().set(HttpExchange.SERVER_ADDRESS, socketAddress);
            if (LOG.isDebugEnabled()) {
                LOG.debug("HttpServer has started on " + socketAddress);
            }
            return aSeqLoop(() -> socket.accept().map(socket1 -> {
                handleConnection(socket1);
                return aTrue();
            })).mapOutcome(outcome -> {
                if (LOG.isDebugEnabled() && isClosed()) {
                    LOG.debug("HttpServer has been stopped: " + socketAddress);
                }
                if (LOG.isDebugEnabled() && outcome.isFailure() && !isClosed()) {
                    LOG.debug("HttpServer has stopped with failure", outcome.failure());
                }
                return aVoid();
            });
        });
    }

    /**
     * Handle a connection.
     *
     * @param connectionSocket the connection socket
     */
    private void handleConnection(final ASocket connectionSocket) {
        final HttpServerConnection connection = new HttpServerConnection(this, connectionSocket, connectionsCount++);
        connections.add(connection);
        connection.run().listen(resolution -> connections.remove(connection));
    }

    /**
     * Fire event that exchange has finished.
     *
     * @param event the event
     */
    public final void fireExchangeFinished(final ExchangeFinishedEvent event) {
        // TODO implement it
        if (LOG.isInfoEnabled()) {
            LOG.info("Exchange finished: " + event);
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopping a server: " + getServerScope().get(HttpExchange.SERVER_ADDRESS));
        }
        return aAll(closeResourceAction(socket)).andLast(
                () -> Streams.aForIterable(connections).consume(connection -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Closing server connection: " + connection.getConnectionId() + " "
                                + connection.getRemoteAddress() + " -> " + connection.getLocalAddress());
                    }
                    return connection.close().observe(
                            LogUtil.logDebugFailures(
                                    LOG, "problem with closing connection: " + connection.getConnectionId())
                    ).mapOutcome(outcome -> aTrue());
                })).toVoid();
    }

    /**
     * @return the normal request handler.
     */
    public AHttpHandler getHandler() {
        return handler;
    }

    /**
     * Set the normal request handler. Note that it is expected to perform all needed request validations,
     * so you probably should use RequestValidator as root, and specify the next handler to be application
     * handler that delegates to .
     *
     * @param handler the handler
     */
    public void setHandler(final AHttpHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("The handler cannot be null");
        }
        this.handler = handler;
    }

    /**
     * @return the handler for the bad requests.
     */
    public AHttpHandler getBadRequestHandler() {
        return badRequestHandler;
    }

    /**
     * Set bad request handler. The handler might get a partial request information (without request info,
     * if the request failed to parse).
     * The handler will contain an exception with key {@link #BAD_REQUEST_PROBLEM} in the scope.
     * The default implementation is {@link BadRequestHandler}, in the most cased it is enough
     * to customize it.
     *
     * @param badRequestHandler the bad request handler
     */
    public void setBadRequestHandler(final AHttpHandler badRequestHandler) {
        if (badRequestHandler == null) {
            throw new IllegalArgumentException("The bad request handler cannot be null");
        }
        this.badRequestHandler = badRequestHandler;
    }
}
