package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

/**
 * <p>A sink for values. The sink is inverse stream, and it is easy to convert from one to another.
 * The stream is pull or sequential side of processing, the sink is parallel or push side.</p>
 * <p>After sink is closed or invalidated, it discards all incoming values.</p>
 *
 * @param <T> the stream element type
 */
public interface ASink<T> extends ACloseable {
    /**
     * Put a value into the sink.
     *
     * @param value the value to put
     * @return the promise that resolves when sink is ready for the next value
     */
    Promise<Void> put(T value);

    /**
     * Write an error to sink, the sink is closed after this method since no more data
     * is expected.
     *
     * @param error the problem
     * @return the promise that resolves when sink is ready for the next value
     */
    Promise<Void> fail(Throwable error);

    /**
     * This method complements {@link ACloseable#close()} method on the {@link AStream} side.
     * this is a way to notify all interested parties that sink is not accepting items
     * anymore. The promise resolves to null in case of success, and fails in case of failure.
     *
     * @return the promise that resolves when sink is no more accepting the items.
     */
    Promise<Void> finished();
}
