package net.sf.asyncobjects.streams;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.OptionalValue;

/**
 * Asynchronous stream of values.
 *
 * @param <T> the stream element type
 */
public interface AStream<T> extends ACloseable {
    /**
     * @return promise for option that is either empty (meaning that EOF is reached, or contains a value)
     */
    Promise<OptionalValue<T>> read();
}
