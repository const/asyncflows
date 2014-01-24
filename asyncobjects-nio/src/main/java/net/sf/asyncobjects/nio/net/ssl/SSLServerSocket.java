package net.sf.asyncobjects.nio.net.ssl;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ChainedClosable;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;

import static net.sf.asyncobjects.core.AsyncControl.aValue;

/**
 * The server socket for SSL.
 */
public class SSLServerSocket extends ChainedClosable<AServerSocket>
        implements AServerSocket, ExportsSelf<AServerSocket> {
    /**
     * The engine factory.
     */
    private final AFunction<SSLEngine, SocketAddress> engineFactory;
    /**
     * The local address.
     */
    private SocketAddress localAddress;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped       the underlying object
     * @param engineFactory the factory for the SSL engine
     */
    public SSLServerSocket(final AServerSocket wrapped, final AFunction<SSLEngine, SocketAddress> engineFactory) {
        super(wrapped);
        this.engineFactory = engineFactory;
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
        return wrapped.bind(address, backlog).map(collectAddress());
    }

    /**
     * This method collects socket address after the bind.
     *
     * @return the void value
     */
    private AFunction<SocketAddress, SocketAddress> collectAddress() {
        return new AFunction<SocketAddress, SocketAddress>() {
            @Override
            public Promise<SocketAddress> apply(final SocketAddress value) throws Throwable {
                localAddress = value;
                return aValue(value);
            }
        };
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        return wrapped.bind(address).map(collectAddress());
    }

    @Override
    public Promise<Void> setDefaultOptions(final SocketOptions options) {
        return wrapped.setDefaultOptions(options);
    }

    @Override
    public Promise<SocketAddress> getLocalSocketAddress() {
        return wrapped.getLocalSocketAddress();
    }

    @Override
    public Promise<ASocket> accept() {
        return wrapped.accept().map(new AFunction<ASocket, ASocket>() {
            @Override
            public Promise<ASocket> apply(final ASocket socket) throws Throwable {
                return engineFactory.apply(localAddress).map(new AFunction<ASocket, SSLEngine>() {
                    @Override
                    public Promise<ASocket> apply(final SSLEngine engine) throws Throwable {
                        final SSLSocket sslSocket = new SSLSocket(socket, engineFactory);
                        return sslSocket.init(engine).thenValue((ASocket) sslSocket.export());
                    }
                });
            }
        });
    }

    @Override
    public AServerSocket export() {
        return export(Vat.current());
    }

    @Override
    public AServerSocket export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }
}
