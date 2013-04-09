package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;

/**
 * Thunk for values.
 *
 * @param <T> the stream element type
 */
public interface AThunk<T> extends ACloseable {
    /**
     * Put a value into the thunk.
     *
     * @param value the value to put
     * @return the promise that resolves when thunk is ready for the next value
     */
    Promise<Void> put(T value);

    /**
     * Fail the thunk. This efficiently closes the thunk, but also notifies other side about
     * a reason for the closing it. So the other side could be aware why no other items
     * are coming.
     *
     * @param problem the problem
     * @return the time when thunk is invalidated.
     */
    Promise<Void> fail(RuntimeException problem);
}
