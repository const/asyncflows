package net.sf.asyncobjects.nio.net.ssl;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.AInput;
import net.sf.asyncobjects.nio.AOutput;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketOptions;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.util.ResourceUtil.closeResource;

/**
 * The SSL socket.
 */
public class SSLSocket extends SSLChannel<ASocket> implements ASSLSocket, ExportsSelf<ASSLSocket> {
    /**
     * The engine factory.
     */
    private final AFunction<SSLEngine, SocketAddress> engineFactory;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped       the underlying object
     * @param engineFactory the {@link SSLEngine} factory.
     */
    public SSLSocket(final ASocket wrapped, final AFunction<SSLEngine, SocketAddress> engineFactory) {
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
        return wrapped.connect(address).thenDo(() -> engineFactory.apply(address).map(this::init));
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
    public ASSLSocket export() {
        return export(Vat.current());
    }

    @Override
    public ASSLSocket export(final Vat vat) {
        final ASSLSocket socket = this;
        return new ASSLSocket() {
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
