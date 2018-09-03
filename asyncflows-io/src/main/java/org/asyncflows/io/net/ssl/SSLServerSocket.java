package org.asyncflows.io.net.ssl;

import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketExportUtil;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.NeedsExport;

import javax.net.ssl.SSLEngine;
import java.net.SocketAddress;

import static org.asyncflows.core.AsyncControl.aValue;

/**
 * The server socket for SSL.
 */
public class SSLServerSocket extends ChainedClosable<AServerSocket>
        implements AServerSocket, NeedsExport<AServerSocket> {
    /**
     * The engine factory.
     */
    private final AFunction<SocketAddress, SSLEngine> engineFactory;
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
    public SSLServerSocket(final AServerSocket wrapped, final AFunction<SocketAddress, SSLEngine> engineFactory) {
        super(wrapped);
        this.engineFactory = engineFactory;
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
        return wrapped.bind(address, backlog).flatMap(collectAddress());
    }

    /**
     * This method collects socket address after the bind.
     *
     * @return the void value
     */
    private AFunction<SocketAddress, SocketAddress> collectAddress() {
        return value -> {
            localAddress = value;
            return aValue(value);
        };
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        return wrapped.bind(address).flatMap(collectAddress());
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
        return wrapped.accept().flatMap(socket -> engineFactory.apply(localAddress).flatMap(engine -> {
            final SSLSocket sslSocket = new SSLSocket(socket, engineFactory);
            return sslSocket.init(engine).thenValue((ASocket) sslSocket.export());
        }));
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
