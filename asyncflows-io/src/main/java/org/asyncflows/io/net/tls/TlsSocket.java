package org.asyncflows.io.net.tls;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AFunction;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.util.CoreFlowsResource.closeResource;

/**
 * The SSL socket.
 */
public class TlsSocket extends TlsChannel<ASocket> implements ATlsSocket, NeedsExport<ATlsSocket> {
    /**
     * The engine factory.
     */
    private final AFunction<SocketAddress, SSLEngine> engineFactory;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped       the underlying object
     * @param engineFactory the {@link SSLEngine} factory.
     */
    public TlsSocket(final ASocket wrapped, final AFunction<SocketAddress, SSLEngine> engineFactory) {
        super(wrapped);
        this.engineFactory = engineFactory;
    }

    @Override
    public Promise<Void> handshake() {
        return doHandshake();
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        return wrapped.setOptions(options);
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        if (getEngine() != null) {
            throw new IllegalStateException("SSLEngine is already initialized!");
        }
        return wrapped.connect(address).thenFlatGet(() -> engineFactory.apply(address).flatMap(this::init));
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return wrapped.getRemoteAddress();
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return wrapped.getLocalAddress();
    }

    @Override
    public ATlsSocket export(final Vat vat) {
        final ATlsSocket socket = this;
        return new ATlsSocket() {
            @Override
            public Promise<Void> handshake() {
                return aLater(vat, socket::handshake);
            }

            @Override
            public Promise<SSLSession> getSession() {
                return aLater(vat, socket::getSession);
            }

            @Override
            public Promise<Void> setOptions(final SocketOptions options) {
                return aLater(vat, () -> socket.setOptions(options));
            }

            @Override
            public Promise<Void> connect(final SocketAddress address) {
                return aLater(vat, () -> socket.connect(address));
            }

            @Override
            public Promise<SocketAddress> getRemoteAddress() {
                return aLater(vat, socket::getRemoteAddress);
            }

            @Override
            public Promise<SocketAddress> getLocalAddress() {
                return aLater(vat, socket::getRemoteAddress);
            }

            @Override
            public Promise<AInput<ByteBuffer>> getInput() {
                return aLater(vat, socket::getInput);
            }

            @Override
            public Promise<AOutput<ByteBuffer>> getOutput() {
                return aLater(vat, socket::getOutput);
            }

            @Override
            public Promise<Void> close() {
                return closeResource(vat, socket);
            }
        };
    }
}
