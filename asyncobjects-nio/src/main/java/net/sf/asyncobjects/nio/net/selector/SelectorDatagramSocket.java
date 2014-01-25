package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.CloseableBase;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.vats.Vat;
import net.sf.asyncobjects.nio.net.ADatagramSocket;
import net.sf.asyncobjects.nio.net.SocketExportUtil;
import net.sf.asyncobjects.nio.net.SocketOptions;
import net.sf.asyncobjects.nio.net.SocketUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The datagram socket for the selector vat.
 */
class SelectorDatagramSocket extends CloseableBase implements ADatagramSocket, ExportsSelf<ADatagramSocket> {
    /**
     * Amount unready receives without result.
     */
    private static final int BROKEN_SELECT_LIMIT = 5;
    /**
     * The queue for receive operations.
     */
    private final RequestQueue receives = new RequestQueue();
    /**
     * The queue for send operations.
     */
    private final RequestQueue sends = new RequestQueue();
    /**
     * The socket datagram channel.
     */
    private final DatagramChannel datagramChannel;
    /**
     * The datagramChannel context.
     */
    private final ChannelContext channelContext;

    /**
     * The constructor.
     *
     * @param selector the selector
     * @throws IOException if problem registering datagram channel
     */
    SelectorDatagramSocket(final Selector selector) throws IOException {
        this(DatagramChannel.open(), selector);
    }


    /**
     * The constructor.
     *
     * @param datagramChannel the datagram channel
     * @param selector        the selector
     * @throws IOException if problem registering datagram channel
     */
    SelectorDatagramSocket(final DatagramChannel datagramChannel, final Selector selector) throws IOException {
        this.datagramChannel = datagramChannel;
        datagramChannel.configureBlocking(false);
        this.channelContext = new ChannelContext(datagramChannel, selector);
    }

    @Override
    public Promise<Void> setOptions(final SocketOptions options) {
        try {
            SocketUtil.applyOptions(datagramChannel.socket(), options);
            return aVoid();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> connect(final SocketAddress address) {
        try {
            datagramChannel.connect(address);
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> disconnect() {
        try {
            datagramChannel.disconnect();
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<SocketAddress> getRemoteAddress() {
        return aValue(datagramChannel.socket().getRemoteSocketAddress());
    }

    @Override
    public Promise<SocketAddress> getLocalAddress() {
        return aValue(datagramChannel.socket().getLocalSocketAddress());
    }

    @Override
    public Promise<SocketAddress> bind(final SocketAddress address) {
        try {
            datagramChannel.socket().bind(address);
            return getLocalAddress();
        } catch (SocketException e) {
            return aFailure(e);
        }
    }

    @Override
    public Promise<Void> send(final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("Empty datagrams are not supported "
                    + "(it is impossible to distinguish not sent and empty)!");
        }
        return sends.runSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                final int write = datagramChannel.write(buffer);
                if (write != 0) {
                    return aFalse();
                }
                return channelContext.waitForWrite().thenValue(true);
            }
        });
    }

    @Override
    public Promise<Void> send(final SocketAddress address, final ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("Empty datagrams are not supported "
                    + "(it is impossible to distinguish not sent and empty)!");
        }
        return sends.runSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                final int write = datagramChannel.send(buffer, address);
                if (write != 0) {
                    return aFalse();
                }
                return channelContext.waitForWrite().thenValue(true);
            }
        });
    }

    @Override
    public Promise<SocketAddress> receive(final ByteBuffer buffer) {
        final int[] count = new int[1];
        return receives.runSeqMaybeLoop(new ACallable<Maybe<SocketAddress>>() {
            @Override
            public Promise<Maybe<SocketAddress>> call() throws Throwable {
                final SocketAddress address = datagramChannel.receive(buffer);
                if (address != null) {
                    return aMaybeValue(address);
                } else {
                    count[0]++;
                    if (count[0] >= BROKEN_SELECT_LIMIT) {
                        count[0] = 0;
                        channelContext.changeSelector();
                    }
                    return channelContext.waitForRead().thenValue(Maybe.<SocketAddress>empty());
                }
            }
        });
    }

    @Override
    protected Promise<Void> closeAction() {
        try {
            datagramChannel.close();
            return super.closeAction();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public ADatagramSocket export() {
        return export(Vat.current());
    }

    @Override
    public ADatagramSocket export(final Vat vat) {
        return SocketExportUtil.export(vat, this);
    }
}
