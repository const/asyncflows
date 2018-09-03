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
