package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.vats.Vat;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The context for the selectable channel.
 */
final class ChannelContext {
    /**
     * The channel.
     */
    private final SelectableChannel channel;
    /**
     * The selector.
     */
    private Selector selector;
    /**
     * The selection key.
     */
    private SelectionKey key;
    /**
     * The read operation in progress.
     */
    private AResolver<Maybe<Object>> read;
    /**
     * The write operation in progress.
     */
    private AResolver<Boolean> write;
    /**
     * The connect operation in progress.
     */
    private AResolver<Boolean> connect;
    /**
     * The accept operation in progress.
     */
    private AResolver<Maybe<Object>> accept;

    /**
     * The constructor.
     *
     * @param channel  the channel.
     * @param selector the selector.
     * @throws ClosedChannelException if channel is closed.
     */
    public ChannelContext(final SelectableChannel channel, final Selector selector) throws ClosedChannelException {
        this.channel = channel;
        setSelector(selector);
    }

    /**
     * @return current ops flag for the key
     */
    private int ops() {
        int rc = 0;
        if (read != null) {
            rc |= SelectionKey.OP_READ;
        }
        if (write != null) {
            rc |= SelectionKey.OP_WRITE;
        }
        if (connect != null) {
            rc |= SelectionKey.OP_CONNECT;
        }
        if (accept != null) {
            rc |= SelectionKey.OP_ACCEPT;
        }
        return rc;
    }

    /**
     * Wait until read is ready. Because read is used mostly in aSeqMaybeLoop(), it returns Maybe.empty() that
     * continues the loop.
     *
     * @param <T> return type
     * @return Maybe.empty() when wait for read finishes
     */
    @SuppressWarnings("unchecked")
    public <T> Promise<Maybe<T>> waitForRead() {
        if (read != null) {
            throw new IllegalStateException("Double waiting for read");
        }
        final Promise<Maybe<T>> promise = new Promise<Maybe<T>>();
        read = (AResolver<Maybe<Object>>) (Object) promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * @return true when wait for write operation finishes
     */
    public Promise<Boolean> waitForWrite() {
        if (write != null) {
            throw new IllegalStateException("Double waiting for write");
        }
        final Promise<Boolean> promise = new Promise<Boolean>();
        write = promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * @return true when wait for connect operation finishes
     */
    public Promise<Boolean> waitForConnect() {
        if (connect != null) {
            throw new IllegalStateException("Double waiting for connect");
        }
        final Promise<Boolean> promise = new Promise<Boolean>();
        connect = promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * Wait until accept is ready. Because read is used mostly in aSeqMaybeLoop(), it returns Maybe.empty() that
     * continues the loop.
     *
     * @param <T> return type
     * @return true wait for accept operation finishes
     */
    @SuppressWarnings("unchecked")
    public <T> Promise<Maybe<T>> waitForAccept() {
        if (accept != null) {
            throw new IllegalStateException("Double waiting for accept");
        }
        final Promise<Maybe<T>> promise = new Promise<Maybe<T>>();
        accept = (AResolver<Maybe<Object>>) (Object) promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * Update ready operations.
     */
    public void updateReady() {
        if (read != null && key.isReadable()) {
            notifySuccess(read, Maybe.empty());
            read = null;
        }
        if (write != null && key.isWritable()) {
            notifySuccess(write, true);
            write = null;
        }
        if (connect != null && key.isConnectable()) {
            notifySuccess(connect, true);
            connect = null;
        }
        if (accept != null && key.isAcceptable()) {
            notifySuccess(accept, Maybe.empty());
            accept = null;
        }
        key.interestOps(ops());
    }

    /**
     * Resolve ready subscribe for other operations.
     */
    private void updateOps() {
        key.interestOps(ops());
    }

    /**
     * @return the selector
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * Change the selector. This method is called when the old selector is dismissed due to the buggy behaviour.
     * So the key is cancelled. Note, after that the channel cannot be used with that selector again because,
     * the selector stores key in the map.
     *
     * @param selector the selector
     * @throws ClosedChannelException if there channel is already closed
     */
    public void setSelector(final Selector selector) throws ClosedChannelException {
        if (this.selector != null) {
            key.cancel();
        }
        this.selector = selector;
        if (selector != null) {
            this.key = channel.register(selector, ops(), this);
        }
    }

    /**
     * Fail all waiting operations on the channel.
     *
     * @param e the exception to use to fail
     */
    public void fail(final Exception e) {
        if (read != null) {
            notifyFailure(read, e);
            read = null;
        }
        if (write != null) {
            notifyFailure(write, e);
            write = null;
        }
        if (connect != null) {
            notifyFailure(connect, e);
            connect = null;
        }
        if (accept != null) {
            notifyFailure(accept, e);
            accept = null;
        }
    }

    /**
     * @return the current selector vat
     */
    private SelectorVat vat() {
        final Vat current = Vat.current();
        if (current instanceof SelectorVat) {
            return (SelectorVat) current;
        }
        throw new IllegalStateException("The current vat is not a selector vat: " + current);
    }

    /**
     * @return the direct buffer
     */
    public ByteBuffer getDirect() {
        return vat().getDirect();
    }

    /**
     * Release direct buffer.
     *
     * @param buffer the buffer to release
     */
    public void releaseDirect(final ByteBuffer buffer) {
        vat().releaseDirect(buffer);
    }

    /**
     * Change selector, this method is called when something fishy is detected with selector.     *
     */
    public void changeSelector() {
        vat().changeSelector();
    }
}
