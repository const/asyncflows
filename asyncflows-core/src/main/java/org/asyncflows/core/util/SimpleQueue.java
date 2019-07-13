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
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.vats.Vat;

import java.util.Deque;
import java.util.LinkedList;

import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * Simple queue implementation.
 *
 * @param <T> the queue element type
 */
public final class SimpleQueue<T> implements AQueue<T>, NeedsExport<AQueue<T>> {
    /**
     * The elements in the queue. Invariant: if elements is non-empty, {@link #resolvers} must be empty.
     */
    private final Deque<T> elements = new LinkedList<>();
    /**
     * The resolvers waiting for value. Invariant: if resolvers is non-empty, {@link #elements} must be empty.
     */
    private final Deque<AResolver<T>> resolvers = new LinkedList<>();

    @Override
    public Promise<T> take() {
        if (elements.isEmpty()) {
            final Promise<T> rc = new Promise<>();
            resolvers.addLast(rc.resolver());
            return rc;
        } else {
            return aValue(elements.removeFirst());
        }
    }

    @Override
    public Promise<Void> put(final T element) {
        if (resolvers.isEmpty()) {
            elements.addLast(element);
        } else {
            notifySuccess(resolvers.removeFirst(), element);
        }
        return aVoid();
    }

    @Override
    public AQueue<T> export(final Vat vat) {
        return AQueueProxyFactory.createProxy(vat, this);
    }
}
