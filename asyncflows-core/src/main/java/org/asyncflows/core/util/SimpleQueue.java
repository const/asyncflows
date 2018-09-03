package org.asyncflows.core.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.function.AResolver;

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
        return UtilExporter.export(vat, this);
    }
}
