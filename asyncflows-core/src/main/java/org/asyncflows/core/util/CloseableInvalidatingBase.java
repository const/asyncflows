/*
 * Copyright (c) 2018 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.core.util;


import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.asyncflows.core.CoreFlows.aFailure;

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
        return resolution -> {
            if (!resolution.isSuccess()) {
                invalidate(resolution.failure());
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
     * @throws Exception if there is in an invalidation
     */
    protected final void ensureValid() throws Exception {
        if (invalidation != null) {
            if (invalidation instanceof Error) {
                throw (Error) invalidation;
            }
            throw (Exception) invalidation;
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
     * @throws Exception if object is closed or invalid.
     */
    protected final void ensureValidAndOpen() throws Exception {
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
