package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;

/**
 * The closeable that could be invalidated.
 */
public class CloseableInvalidatingBase extends CloseableBase {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CloseableInvalidatingBase.class);
    /**
     * The outcome checker.
     */
    private AResolver<Object> checkOutcomeObserver;
    /**
     * The invalidation.
     */
    private Throwable invalidation;

    /**
     * Invalidate the resource.
     *
     * @param throwable the problem to invalidate with
     */
    protected final void invalidate(final Throwable throwable) {
        if (invalidation == null) {
            invalidation = throwable;
            try {
                onInvalidation(throwable);
            } catch (Throwable t) {
                LOG.error("Invalidation callback failed with error", t);
            }
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
        if (checkOutcomeObserver == null) {
            checkOutcomeObserver = createOutcomeChecker();
        }
        return checkOutcomeObserver;
    }

    /**
     * @return a checker for outcome
     */
    protected AResolver<Object> createOutcomeChecker() {
        return new AResolver<Object>() {
            @Override
            public void resolve(final Outcome<Object> resolution) throws Throwable {
                if (!resolution.isSuccess()) {
                    invalidate(resolution.failure());
                }
            }
        };
    }

    /**
     * @return true if the object is still valid
     */
    protected final boolean isValid() {
        return invalidation == null;
    }

    /**
     * If stream is closed, throw an exception.
     */
    protected void ensureOpen() {
        if (isClosed()) {
            throw new ResourceClosedException("The object is closed", invalidation);
        }
    }

    /**
     * Ensure that the object is still valid.
     *
     * @throws Throwable if there is in an invalidation
     */
    protected final void ensureValid() throws Throwable {
        if (invalidation != null) {
            throw invalidation;
        }
    }

    /**
     * @return true if the resource is still valid and open
     */
    protected final boolean isValidAndOpen() {
        return isValid() && isOpen();
    }

    /**
     * Ensure that the object is still valid and open.
     *
     * @throws Throwable if object is closed or invalid.
     */
    protected final void ensureValidAndOpen() throws Throwable {
        ensureValid();
        ensureOpen();
    }

    /**
     * The invalidation promise. It always returns a failure.
     *
     * @param <A> the promise type.
     * @return the failure promise.
     */
    protected final <A> Promise<A> invalidationPromise() {
        try {
            ensureValidAndOpen();
        } catch (Throwable throwable) {
            return aFailure(throwable);
        }
        return aFailure(new IllegalStateException("The object is neither invalid nor closed"));
    }

    /**
     * @return the invalidation value
     */
    protected final Throwable invalidation() {
        return invalidation;
    }
}
