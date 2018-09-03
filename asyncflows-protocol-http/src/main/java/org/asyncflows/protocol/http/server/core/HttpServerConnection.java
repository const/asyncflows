package org.asyncflows.protocol.http.server.core;

import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.tls.ATlsSocket;
import org.asyncflows.io.util.ByteGeneratorContext;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.core.Promise;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.net.SocketAddress;

import static org.asyncflows.core.CoreFlows.aNull;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The wrapper for HTTP server connection.
 */
class HttpServerConnection extends CloseableBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerConnection.class);
    /**
     * The HTTP handler.
     */
    private final HttpServer server;
    /**
     * The ID of the connection.
     */
    private final long connectionId;
    /**
     * The socket (possibly SSL one).
     */
    private final ASocket socket;
    /**
     * The remote address.
     */
    private SocketAddress remoteAddress;
    /**
     * The remote address.
     */
    private SocketAddress localAddress;
    /**
     * The input.
     */
    private ByteParserContext input;
    /**
     * The output.
     */
    private ByteGeneratorContext output;
    /**
     * The connection protocol (http or https).
     */
    private String protocol;
    /**
     * The established ssl session (if exists).
     */
    private SSLSession sslSession;
    /**
     * Counter for exchanges on the the connection.
     */
    private long exchangeCount;


    /**
     * The constructor.
     *
     * @param server       the http server
     * @param socket       the socket
     * @param connectionId the connection number
     */
    public HttpServerConnection(final HttpServer server, final ASocket socket, final long connectionId) {
        this.socket = socket;
        this.server = server;
        this.connectionId = connectionId;
    }

    /**
     * @return handle all requests on the connection
     */
    public Promise<Void> run() {
        return init().thenFlatGet(() -> {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Started " + protocol + " connection " + connectionId
                        + " on " + remoteAddress + " -> " + localAddress);
            }
            return aSeqWhile(() -> {
                final HttpExchangeAction handler = new HttpExchangeAction(HttpServerConnection.this,
                        exchangeCount++);
                return handler.handle();
            });
        }).flatMapOutcome(value -> {
            final String message = "Finished " + protocol + " connection " + connectionId
                    + " on " + remoteAddress + " -> " + localAddress;
            if (value.isFailure()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message + " with errors", value.failure());
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message);
                }
            }
            return close();
        });
    }

    @Override
    protected Promise<Void> closeAction() {
        return socket.close();
    }

    /**
     * Initialize the connection.
     *
     * @return the connection properties.
     */
    private Promise<Void> init() {
        final SocketOptions socketOptions = new SocketOptions();
        socketOptions.setTpcNoDelay(true);
        return aAll(
                () -> aAll(
                        socket::getInput
                ).and(
                        socket::getOutput
                ).and(() -> {
                    if (socket instanceof ATlsSocket) {
                        return ((ATlsSocket) socket).getSession();
                    } else {
                        return aNull();
                    }
                }).map((inputStream, outputStream, session) -> {
                    input = new ByteParserContext(inputStream, server.getHttpBufferSize());
                    output = new ByteGeneratorContext(outputStream, server.getHttpBufferSize());
                    sslSession = session;
                    return aVoid();
                })
        ).and(
                () -> aAll(
                        () -> socket.setOptions(socketOptions)
                ).and(
                        socket::getRemoteAddress
                ).and(
                        socket::getLocalAddress
                ).map((voidValue, remoteAddressValue, localAddressValue) -> {
                    remoteAddress = remoteAddressValue;
                    localAddress = localAddressValue;
                    return aVoid();
                })
        ).andLast(() -> {
            if (socket instanceof ATlsSocket) {
                return ((ATlsSocket) socket).getSession().flatMap(value -> {
                    sslSession = value;
                    protocol = "https";
                    return aVoid();
                });
            } else {
                protocol = "http";
                return aVoid();
            }
        }).toVoid().listen(LogUtil.logFailures(LOG, "Connection initialization failed"));
    }

    /**
     * @return the http server that manages this connection.
     */
    public HttpServer getServer() {
        return server;
    }

    /**
     * @return the remote address (hop only)
     */
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * @return the local address (hop only)
     */
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * @return the connection input
     */
    public ByteParserContext getInput() {
        return input;
    }

    /**
     * @return the connection output
     */
    public ByteGeneratorContext getOutput() {
        return output;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the SSL session or null if non-SSL socket
     */
    public SSLSession getSslSession() {
        return sslSession;
    }

    /**
     * @return the connection id
     */
    public long getConnectionId() {
        return connectionId;
    }
}
