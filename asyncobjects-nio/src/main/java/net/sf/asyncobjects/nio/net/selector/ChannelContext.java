package net.sf.asyncobjects.nio.net.selector;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
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
    private AResolver<Void> read;
    /**
     * The write operation in progress.
     */
    private AResolver<Void> write;
    /**
     * The connect operation in progress.
     */
    private AResolver<Void> connect;
    /**
     * The accept operation in progress.
     */
    private AResolver<Void> accept;

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
     * @return wait for read operation
     */
    public Promise<Void> waitForRead() {
        if (read != null) {
            throw new IllegalStateException("Double waiting for read");
        }
        if (key.isReadable()) {
            return aVoid();
        }
        final Promise<Void> promise = new Promise<Void>();
        read = promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * @return wait for read operation
     */
    public Promise<Void> waitForWrite() {
        if (write != null) {
            throw new IllegalStateException("Double waiting for write");
        }
        if (key.isWritable()) {
            return aVoid();
        }
        final Promise<Void> promise = new Promise<Void>();
        write = promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * @return wait for read operation
     */
    public Promise<Void> waitForConnect() {
        if (connect != null) {
            throw new IllegalStateException("Double waiting for connect");
        }
        if (key.isConnectable()) {
            return aVoid();
        }
        final Promise<Void> promise = new Promise<Void>();
        connect = promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * @return wait for read operation
     */
    public Promise<Void> waitForAccept() {
        if (accept != null) {
            throw new IllegalStateException("Double waiting for accept");
        }
        if (key.isAcceptable()) {
            return aVoid();
        }
        final Promise<Void> promise = new Promise<Void>();
        accept = promise.resolver();
        updateOps();
        return promise;
    }

    /**
     * Update ready operations.
     */
    public void updateReady() {
        if (read != null && key.isReadable()) {
            notifySuccess(read, null);
            read = null;
        }
        if (write != null && key.isWritable()) {
            notifySuccess(write, null);
            write = null;
        }
        if (connect != null && key.isConnectable()) {
            notifySuccess(connect, null);
            connect = null;
        }
        if (accept != null && key.isAcceptable()) {
            notifySuccess(accept, null);
            accept = null;
        }
        key.interestOps(ops());
    }

    /**
     * Resolve ready subscribe for other operations.
     */
    private void updateOps() {
        updateReady();
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
