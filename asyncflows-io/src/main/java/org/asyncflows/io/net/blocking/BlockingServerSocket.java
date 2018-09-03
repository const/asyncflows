package org.asyncflows.io.net.blocking;

import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.io.net.AServerSocket;
import org.asyncflows.io.net.ASocket;
import org.asyncflows.io.net.SocketExportUtil;
import org.asyncflows.io.net.SocketOptions;
import org.asyncflows.io.net.SocketUtil;
import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.util.CloseableInvalidatingBase;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.AsyncControl.aVoid;

/**
 * The blocking server socket implementation. Note that by default it exports on the daemon vat.
 */
public class BlockingServerSocket extends CloseableInvalidatingBase
        implements AServerSocket, NeedsExport<AServerSocket> {
    /**
     * The wrapped socket.
     */
    private final ServerSocket serverSocket;
    /**
     * The socket options.
     */
    private SocketOptions options;

    /**
     * The constructor.
     *
     * @throws IOException if there is a problem with creating socket
     */
    public BlockingServerSocket() throws IOException {
        serverSocket = new ServerSocket();
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
        try {
            serverSocket.bind(address, backlog);
            return getLocalSocketAddress();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        try {
            serverSocket.bind(address);
            return getLocalSocketAddress();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> setDefaultOptions(final SocketOptions newOptions) {
        if (newOptions != null) {
            options = newOptions.clone();
        } else {
            options = null;
        }
        return aVoid();
    }

    @Override
    public Promise<SocketAddress> getLocalSocketAddress() {
        try {
            return aValue(serverSocket.getLocalSocketAddress());
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<ASocket> accept() {
        try {
            final Socket accepted = serverSocket.accept();
            try {
                if (options != null) {
                    SocketUtil.applyOptions(accepted, options);
                }
            } catch (Throwable t) {
                accepted.close();
                return aFailure(t);
            }
            return aValue(new BlockingSocket(accepted).export());
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            serverSocket.close();
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public AServerSocket export() {
        return SocketExportUtil.export(Vats.daemonVat(), Vats.daemonVat(), this);
    }

    @Override
    public AServerSocket export(final Vat vat) {
        return export();
    }
}
