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

package org.asyncflows.core.util.sample;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.util.ObjectExporter;
import org.asyncflows.core.vats.Vat;

import java.util.Deque;
import java.util.LinkedList;

import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.Outcome.notifySuccess;

public class TestQueue<T> implements ATestQueue<T>, NeedsExport<ATestQueue<T>> {
    private final Deque<T> elements = new LinkedList<>();
    private final Deque<AResolver<T>> resolvers = new LinkedList<>();

    private void invariantCheck() {
        // checks that queue invariant holds
        if(!elements.isEmpty() && !resolvers.isEmpty()) {
            throw new RuntimeException("BUG: one of the collections should be empty");
        }
    }

    @Override
    public Promise<T> take() {
        invariantCheck();
        if (elements.isEmpty()) {
            return aResolver(r -> {
                resolvers.addLast(r);
            });
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
        return ObjectExporter.export(vat, this);
    }
}
