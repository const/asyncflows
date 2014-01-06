package net.sf.asyncobjects.nio.net.blocking;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableInvalidatingBase;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.core.vats.Vats;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;
import net.sf.asyncobjects.nio.net.SocketUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The blocking server socket implementation. Note that by default it exports on the daemon vat.
 */
public class BlockingServerSocket extends CloseableInvalidatingBase
        implements AServerSocket, ExportsSelf<AServerSocket> {
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
    public Promise<Void> bind(final SocketAddress address, final int backlog) {
        try {
            serverSocket.bind(address, backlog);
            return aVoid();
        } catch (Throwable e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> bind(final SocketAddress address) {
        try {
            serverSocket.bind(address);
            return aVoid();
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
    public AServerSocket export() {
        return export(Vats.daemonVat());
    }

    @Override
    public AServerSocket export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }
}
