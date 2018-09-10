package org.asyncflows.protocol.http.server.core;

import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.protocol.http.common.Scope;
import org.asyncflows.protocol.http.common.headers.HttpHeadersUtil;
import org.asyncflows.protocol.http.server.AHttpHandler;
import org.asyncflows.protocol.http.server.HttpExchange;
import org.asyncflows.protocol.http.server.util.DelegatingHandler;
import org.asyncflows.core.Promise;
import org.asyncflows.core.streams.AsyncStreams;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.core.util.CoreFlowsResource.closeResourceAction;

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
        return socket.getLocalSocketAddress().flatMap(socketAddress -> {
            getServerScope().set(HttpExchange.SERVER_ADDRESS, socketAddress);
            if (LOG.isDebugEnabled()) {
                LOG.debug("HttpServer has started on " + socketAddress);
            }
            return aSeqWhile(() -> socket.accept().flatMap(socket1 -> {
                handleConnection(socket1);
                return aTrue();
            })).flatMapOutcome(outcome -> {
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
                () -> AsyncStreams.aForIterable(connections).consume(connection -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Closing server connection: " + connection.getConnectionId() + " "
                                + connection.getRemoteAddress() + " -> " + connection.getLocalAddress());
                    }
                    return connection.close().listen(
                            LogUtil.logDebugFailures(
                                    LOG, "problem with closing connection: " + connection.getConnectionId())
                    ).flatMapOutcome(outcome -> aTrue());
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