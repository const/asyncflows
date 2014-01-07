package net.sf.asyncobjects.nio.net.ssl;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.ASocketFactory;
import net.sf.asyncobjects.nio.net.SocketExportUtil;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;

import static net.sf.asyncobjects.core.AsyncControl.aValue;

/**
 * The SSL socket factory.
 */
public class SSLSocketFactory implements ASocketFactory, ExportsSelf<ASocketFactory> {
    /**
     * The factory for server SSL engine.
     */
    private AFunction<SSLEngine, SocketAddress> serverEngineFactory;
    /**
     * The factory for client SSL engine.
     */
    private AFunction<SSLEngine, SocketAddress> clientEngineFactory;
    /**
     * The underlying socket factory.
     */
    private ASocketFactory socketFactory;

    /**
     * @return the promise for a plain socket
     */
    @Override
    public Promise<ASocket> makeSocket() {
        return getSocketFactory().makeSocket().map(new AFunction<ASocket, ASocket>() {
            @Override
            public Promise<ASocket> apply(final ASocket value) throws Throwable {
                return aValue((ASocket) new SSLSocket(value, getClientEngineFactory()).export());
            }
        });
    }

    /**
     * @return the promise for server socket
     */
    @Override
    public Promise<AServerSocket> makeServerSocket() {
        return getSocketFactory().makeServerSocket().map(new AFunction<AServerSocket, AServerSocket>() {
            @Override
            public Promise<AServerSocket> apply(final AServerSocket value) throws Throwable {
                return aValue(new SSLServerSocket(value, getServerEngineFactory()).export());
            }
        });
    }

    /**
     * @return the factory for the SSL engine on the server side
     */
    public AFunction<SSLEngine, SocketAddress> getServerEngineFactory() {
        return serverEngineFactory;
    }

    /**
     * Set factory for server side SSL engine.
     *
     * @param serverEngineFactory the SSL engine
     */
    public void setServerEngineFactory(final AFunction<SSLEngine, SocketAddress> serverEngineFactory) {
        this.serverEngineFactory = serverEngineFactory;
    }

    /**
     * @return the client SSLEngine factory
     */
    public AFunction<SSLEngine, SocketAddress> getClientEngineFactory() {
        return clientEngineFactory;
    }

    /**
     * Set factory for client SSL engine.
     *
     * @param clientEngineFactory the ssl engine
     */
    public void setClientEngineFactory(final AFunction<SSLEngine, SocketAddress> clientEngineFactory) {
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
    public ASocketFactory export() {
        return export(Vat.current());
    }

    @Override
    public ASocketFactory export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }
}