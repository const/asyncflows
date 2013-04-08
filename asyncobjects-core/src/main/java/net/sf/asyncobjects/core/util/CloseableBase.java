package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.Promise;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * The base for closeable.
 */
public abstract class CloseableBase implements ACloseable {
    /**
     * The close promise.
     */
    private Promise<Void> closePromise;

    /**
     * @return true if stream is closed
     */
    protected final boolean isClosed() {
        return closePromise != null;
    }

    /**
     * @return true if stream is open
     */
    protected final boolean isOpen() {
        return !isClosed();
    }

    /**
     * If stream is closed, throw an exception.
     */
    protected void ensureOpen() {
        if (isClosed()) {
            throw new IllegalStateException("The object is closed");
        }
    }

    /**
     * The close action. Override it to implement a custom close operation.
     *
     * @return promise that resolves when close is complete
     */
    protected Promise<Void> closeAction() {
        return aVoid();
    }

    @Override
    public Promise<Void> close() {
        if (closePromise == null) {
            try {
                closePromise = closeAction();
            } catch (Throwable ex) {
                closePromise = aFailure(ex);
            }
        }
        return closePromise;
    }
}
