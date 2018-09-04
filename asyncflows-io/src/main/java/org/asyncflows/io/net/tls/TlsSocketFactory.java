package org.asyncflows.io.net.tls;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.net.ADatagramSocket;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.ASocketFactory;
import org.asyncflows.io.net.SocketExportUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AFunction;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;

/**
 * The TLS socket factory.
 */
public class TlsSocketFactory implements ASocketFactory, NeedsExport<ASocketFactory> {
    /**
     * The factory for server SSL engine.
     */
    private AFunction<SocketAddress, SSLEngine> serverEngineFactory;
    /**
     * The factory for client SSL engine.
     */
    private AFunction<SocketAddress, SSLEngine> clientEngineFactory;
    /**
     * The underlying socket factory.
     */
    private ASocketFactory socketFactory;

    @Override
    public Promise<ASocket> makeSocket() {
        return getSocketFactory().makeSocket().map(
                value -> new TlsSocket(value, getClientEngineFactory()).export());
    }

    @Override
    public Promise<AServerSocket> makeServerSocket() {
        return getSocketFactory().makeServerSocket().map(
                value -> new TlsServerSocket(value, getServerEngineFactory()).export());
    }

    @Override
    public Promise<ADatagramSocket> makeDatagramSocket() {
        throw new UnsupportedOperationException("DTLS sockets are not supported yet!");
    }

    /**
     * @return the factory for the SSL engine on the server side
     */
    public AFunction<SocketAddress, SSLEngine> getServerEngineFactory() {
        return serverEngineFactory;
    }

    /**
     * Set factory for server side SSL engine.
     *
     * @param serverEngineFactory the SSL engine
     */
    public void setServerEngineFactory(final AFunction<SocketAddress, SSLEngine> serverEngineFactory) {
        this.serverEngineFactory = serverEngineFactory;
    }

    /**
     * @return the client SSLEngine factory
     */
    public AFunction<SocketAddress, SSLEngine> getClientEngineFactory() {
        return clientEngineFactory;
    }

    /**
     * Set factory for client SSL engine.
     *
     * @param clientEngineFactory the ssl engine
     */
    public void setClientEngineFactory(final AFunction<SocketAddress, SSLEngine> clientEngineFactory) {
        this.clientEngineFactory = clientEngineFactory;
    }

    /**
     * @return the underlying socket factory
     */
    public ASocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
     * Set socket factory.
     *
     * @param socketFactory the socket factory
     */
    public void setSocketFactory(final ASocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public ASocketFactory export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }
}
