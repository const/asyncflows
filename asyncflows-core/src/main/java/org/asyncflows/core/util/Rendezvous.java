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

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.util.CancellableFlows.aCancellable;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * Hand-off queue component, that transfers value from one side to another
 * if read and write request meets together. The operations on this
 * queue are cancellable, but queue prefers to complete operations
 * normally to cancellation if it is possible.
 *
 * @param <T> the element type
 */
public class Rendezvous<T> implements AQueue<T>, NeedsExport<AQueue<T>> {
    private final RequestQueue reads = new RequestQueue();
    private final RequestQueue writes = new RequestQueue();
    private AResolver<T> takeResolver;

    @Override
    public Promise<T> take() {
        return reads.run(() -> aCancellable(() -> CoreFlows.<T>aResolver(r -> {
            takeResolver = r;
            writes.resume();
        })).onCancel(t -> {
            if (takeResolver != null) {
                notifyFailure(takeResolver, t);
                takeResolver = null;
                writes.resume();
            }
        }));
    }

    @Override
    public Promise<Void> put(T element) {
        final Cell<Throwable> cancellation = new Cell<>();
        return writes.run(() -> aCancellable(() -> aSeqWhile(() -> {
            if (takeResolver != null) {
                // if resolver presents, then we return normally
                notifySuccess(takeResolver, element);
                takeResolver = null;
                reads.resume();
                return aFalse();
            } else if (!cancellation.isEmpty()) {
                // no resolver, and cancellation is requested, aborting
                return aFailure(cancellation.getValue());
            } else {
                // wait for some event
                return writes.suspendThenTrue();
            }
        })).onCancel(t -> {
            cancellation.setValue(t);
            writes.resume();
        }));
    }

    @Override
    public AQueue<T> export(Vat vat) {
        return AQueueProxyFactory.createProxy(vat, this);
    }
}
