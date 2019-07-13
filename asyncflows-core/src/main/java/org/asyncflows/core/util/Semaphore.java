/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;

/**
 * Asynchronous semaphore class.
 */
public final class Semaphore implements ASemaphore, NeedsExport<ASemaphore> {
    /**
     * The acquire requests.
     */
    private final RequestQueue requests = new RequestQueue();
    /**
     * The permits.
     */
    private int permits;

    /**
     * The constructor.
     *
     * @param permits amount of permits
     */
    public Semaphore(final int permits) {
        this.permits = permits;
    }

    @Override
    public void release(final int releasedPermits) {
        if (releasedPermits <= 0) {
            return;
        }
        permits += releasedPermits;
        requests.resume();
    }

    @Override
    public void release() {
        release(1);
    }

    @Override
    public Promise<Void> acquire() {
        return acquire(1);
    }

    @Override
    public Promise<Void> acquire(final int requestedPermits) {
        if (requestedPermits <= 0) {
            return aFailure(new IllegalArgumentException("The requestedPermits must be positive: " + requestedPermits));
        }
        return requests.runSeqWhile(() -> {
            if (requestedPermits <= permits) {
                permits -= requestedPermits;
                return aFalse();
            } else {
                return requests.suspendThenTrue();
            }
        });
    }

    /**
     * Run action with single permit.
     *
     * @param supplier a supplier
     * @param <T>      the action
     * @return result of action
     */
    public <T> Promise<T> run(final ASupplier<T> supplier) {
        return acquire().thenFlatGet(supplier).listen(o -> release());
    }

    @Override
    public ASemaphore export(final Vat vat) {
        return ASemaphoreProxyFactory.createProxy(vat, this);
    }
}
