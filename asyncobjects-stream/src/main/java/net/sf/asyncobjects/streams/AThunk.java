package net.sf.asyncobjects.streams;

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
    Promise<Void> write(T value);
}
