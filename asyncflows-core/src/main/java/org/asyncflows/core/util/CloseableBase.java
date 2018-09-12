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
    /**
     * True if closing is in progress.
     */
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
