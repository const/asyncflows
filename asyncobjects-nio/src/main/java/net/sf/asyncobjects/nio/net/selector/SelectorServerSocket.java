package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.net.AServerSocket;
import net.sf.asyncobjects.nio.net.ASocket;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * Server socket based on selectors.
 */
class SelectorServerSocket extends CloseableBase implements AServerSocket, ExportsSelf<AServerSocket> {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SelectorServerSocket.class);
    /**
     * The context for the channel.
     */
    private final ChannelContext channelContext;
    /**
     * The channel for the server socket.
     */
    private final ServerSocketChannel serverSocketChannel;
    /**
     * The request queue for the accept operation.
     */
    private final RequestQueue queue = new RequestQueue();
    /**
     * Default socket options for accepted sockets.
     */
    private SocketOptions defaultSocketOptions;

    /**
     * The constructor.
     *
     * @param selector the selector
     * @throws IOException the exception
     */
    public SelectorServerSocket(final Selector selector) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);
        this.channelContext = new ChannelContext(serverSocketChannel, selector);
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address, final int backlog) {
        try {
            serverSocketChannel.socket().bind(address, backlog);
            return getLocalSocketAddress();
        } catch (IOException ex) {
            return aFailure(ex);
        }
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        try {
            serverSocketChannel.socket().bind(address);
            return getLocalSocketAddress();
        } catch (IOException ex) {
            return aFailure(ex);
        }
    }

    @Override
    public Promise<Void> setDefaultOptions(final SocketOptions options) {
        this.defaultSocketOptions = options;
        return aVoid();
    }

    @Override
    public Promise<SocketAddress> getLocalSocketAddress() {
        return aValue(serverSocketChannel.socket().getLocalSocketAddress());
    }

    @Override
    public Promise<ASocket> accept() {
        return queue.runSeqMaybeLoop(() -> {
            ensureOpen();
            final SocketChannel accepted = serverSocketChannel.accept();
            if (accepted == null) {
                return channelContext.waitForAccept();
            }
            final SelectorSocket socket = new SelectorSocket(channelContext.getSelector(), accepted);
            if (defaultSocketOptions != null) {
                socket.setOptions(defaultSocketOptions);
            }
            return aMaybeValue(socket.export());
        });
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            serverSocketChannel.close();
            return aVoid();
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failure while closing server socket", e);
            }
            return aFailure(e);
        } finally {
            channelContext.close();
        }
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
