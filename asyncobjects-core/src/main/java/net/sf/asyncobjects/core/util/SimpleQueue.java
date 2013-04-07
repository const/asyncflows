package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.ExportsSelf;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.vats.Vat;

import java.util.Deque;
import java.util.LinkedList;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * Simple queue implementation.
 *
 * @param <T> the queue element type
 */
public final class SimpleQueue<T> implements AQueue<T>, ExportsSelf<AQueue<T>> {
    /**
     * The elements in the queue. Invariant: if elements is non-empty, {@link #resolvers} must be empty.
     */
    private final Deque<T> elements = new LinkedList<T>();
    /**
     * The resolvers waiting for value. Invariant: if resolvers is non-empty, {@link #elements} must be empty.
     */
    private final Deque<AResolver<T>> resolvers = new LinkedList<AResolver<T>>();

    @Override
    public Promise<T> take() {
        if (elements.isEmpty()) {
            final Promise<T> rc = new Promise<T>();
            resolvers.addLast(rc.resolver());
            return rc;
        } else {
            return aSuccess(elements.removeFirst());
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
        return UtilExportUtil.export(vat, this);
    }

    @Override
    public AQueue<T> export() {
        return export(Vat.current());
    }
}
