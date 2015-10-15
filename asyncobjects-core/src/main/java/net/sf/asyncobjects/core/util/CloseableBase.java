package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Promise;

import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;

/**
 * The base for closeable.
 */
public abstract class CloseableBase implements ACloseable {
    /**
     * The close promise.
     */
    private final Promise<Void> closePromise = new Promise<>();

    /**
     * @return true if stream is closed
     */
    public final boolean isClosed() {
        return closePromise.getState() != Promise.State.INITIAL;
    }

    /**
     * @return true if stream is open
     */
    public final boolean isOpen() {
        return !isClosed();
    }

    /**
     * If stream is closed, throw an exception.
     */
    protected void ensureOpen() {
        if (isClosed()) {
            throw new ResourceClosedException("The object is closed");
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
        startClosing();
        return closePromise;
    }

    /**
     * Start closing a stream if needed.
     */
    protected void startClosing() {
        if (isOpen()) {
            final AResolver<Void> resolver = closePromise.resolver();
            try {
                closeAction().listen(resolver);
            } catch (Throwable problem) {
                notifyFailure(resolver, problem);
            }
        }
    }
}
