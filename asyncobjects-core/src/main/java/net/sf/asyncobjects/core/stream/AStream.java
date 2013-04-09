package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.util.ACloseable;
import net.sf.asyncobjects.core.util.OptionalValue;

/**
 * Asynchronous stream of values. The stream auto-closes when end of the stream is reached or when
 * failure is encountered.
 *
 * @param <T> the stream element type
 */
public interface AStream<T> extends ACloseable {
    /**
     * @return promise for option that is either empty (meaning that EOF is reached, or contains a value)
     */
    Promise<OptionalValue<T>> next();
}
