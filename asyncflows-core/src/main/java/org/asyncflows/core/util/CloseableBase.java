package org.asyncflows.core.util;


import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AResolver;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;

/**
 * The base for closeable.
 */
public abstract class CloseableBase implements ACloseable {
    /**
     * The close promise.
     */
    private final Promise<Void> closePromise = new Promise<>();
    private boolean isClosing;

    /**
     * @return true if stream is closed
     */
    public final boolean isClosed() {
        return !closePromise.isUnresolved();
    }

    /**
     * @return true if stream is open
     */
    public final boolean isOpen() {
        return !isClosed() && !isClosing;
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
            isClosing = true;
            final AResolver<Void> resolver = closePromise.resolver();
            try {
                closeAction().listen(resolver);
            } catch (Throwable problem) {
                notifyFailure(resolver, problem);
            }
        }
    }
}
