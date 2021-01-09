/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.core.util.sample;

import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.Outcome.notifySuccess;

import java.util.Deque;
import java.util.LinkedList;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.vats.Vat;

public class TestQueue<T> implements ATestQueue<T>, ExportableComponent<ATestQueue<T>> {
    private final Deque<T> elements = new LinkedList<>();
    private final Deque<AResolver<T>> resolvers = new LinkedList<>();

    private void invariantCheck() {
        // checks that queue invariant holds
        if (!elements.isEmpty() && !resolvers.isEmpty()) {
            throw new IllegalStateException("BUG: at least one of the collections should be empty");
        }
    }

    @Override
    public Promise<T> take() {
        invariantCheck();
        if (elements.isEmpty()) {
            return aResolver(resolvers::addLast);
        } else {
            return aValue(elements.removeFirst());
        }
    }

    @Override
    public void put(final T element) {
        invariantCheck();
        if (resolvers.isEmpty()) {
            elements.addLast(element);
        } else {
            notifySuccess(resolvers.removeFirst(), element);
        }
    }

    @Override
    public ATestQueue<T> export(final Vat vat) {
        return ATestQueueProxyFactory.createProxy(vat, this);
    }
}
