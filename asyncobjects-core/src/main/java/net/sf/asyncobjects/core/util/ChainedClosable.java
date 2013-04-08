package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;

/**
 * A Closeable instance above other closeable instance.
 *
 * @param <T> the underlying object type
 */
public abstract class ChainedClosable<T extends ACloseable> implements ACloseable {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ChainedClosable.class);
    /**
     * If true, the close operation is already started.
     */
    private boolean closeStarted;
    /**
     * The promise for close operation.
     */
    private final Promise<Void> closePromise = new Promise<Void>();
    /**
     * The resolver for close promise.
     */
    private final AResolver<Void> closePromiseResolver = closePromise.resolver();
    /**
     * The outcome checker.
     */
    private final AResolver<Object> checkOutcomeResolver = new AResolver<Object>() {
        @Override
        public void resolve(final Outcome<Object> resolution) throws Throwable {
            if (!resolution.isSuccess()) {
                invalidate(resolution.failure());
            }
        }
    };
    /**
     * The invalidation.
     */
    private Throwable invalidation;

    /**
     * The wrapped closeable object.
     */
    protected final T wrapped;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedClosable(final T wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Start closing the underlying object.
     */
    protected void startClosing() {
        if (!closeStarted) {
            closeStarted = true;
            aSeq(new ACallable<Void>() {
                @Override
                public Promise<Void> call() throws Throwable {
                    return beforeClose();
                }
            }).finallyDo(ResourceUtil.closeResourceAction(wrapped)).listen(closePromiseResolver);
        }
    }

    /**
     * @return before close action
     */
    protected Promise<Void> beforeClose() {
        return aVoid();
    }

    /**
     * Invalidate the stream.
     *
     * @param throwable the problem to invalidate with
     */
    protected final void invalidate(final Throwable throwable) {
        invalidation = throwable;
        try {
            onInvalidation(throwable);
        } catch (Throwable t) {
            LOG.error("Invalidation callback failed with error", t);
        }
    }

    /**
     * The invalidation callback.
     *
     * @param throwable the invalidation reason
     */
    protected void onInvalidation(final Throwable throwable) { // NOPMD
        // it will be overridden in subclasses if needed
    }

    /**
     * @return listener that invalidates the object depending on the outcome
     */
    protected final AResolver<Object> outcomeChecker() {
        return checkOutcomeResolver;
    }

    /**
     * @return true if the object is not a valid and open, in that case the next action will
     *         be likely throwing or returning a failure.
     */
    protected final boolean isNotValidAndOpen() {
        return closeStarted || invalidation != null;
    }

    /**
     * @return check if closeable is still valid
     */
    protected final boolean isValid() {
        return invalidation == null;
    }

    /**
     * This method returns a failure with a reason for invalidation of the stream.
     * The method is used in conjunction with {@link #isNotValidAndOpen()}
     *
     * @param <A> the failure type
     * @return the failure promise
     */
    protected final <A> Promise<A> failureInvalidOrClosed() {
        if (invalidation != null) {
            return aFailure(invalidation);
        } else if (closeStarted) {
            return aFailure(new IllegalStateException("The closeable is closed"));
        } else {
            return aFailure(new IllegalStateException("The closeable is neither invalid nor closed"));
        }
    }

    /**
     * @return true if close is not yet started
     */
    protected boolean isOpen() {
        return !closeStarted;
    }


    @Override
    public Promise<Void> close() {
        startClosing();
        return closePromise;
    }
}
