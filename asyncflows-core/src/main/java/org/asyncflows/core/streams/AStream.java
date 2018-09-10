package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ACloseable;

/**
 * Asynchronous stream of values. The stream elements could be success values or failures. It is up to stream
 * how it handles the failures. The stream generally accepts multiple close operations.
 *
 * @param <T> the stream element type
 */
public interface AStream<T> extends ACloseable {
    /**
     * <p>The next element in the stream. Note that the stream might contain errors. After EOF is returned,
     * all other reads will return EOF. If stream is closed, the next operation will throw an exception.</p>
     *
     * @return promise for option that is either empty (meaning that EOF is reached), or contains a value
     */
    Promise<Maybe<T>> next();
}