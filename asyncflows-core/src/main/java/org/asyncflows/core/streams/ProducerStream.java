package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;

/**
 * The base for simple producer stream that delegates to {@link ASupplier}.
 *
 * @param <A> the stream element type
 */
public class ProducerStream<A> extends StreamBase<A> {
    /**
     * The producer for the values.
     */
    private final ASupplier<Maybe<A>> producer;

    /**
     * The producer stream.
     *
     * @param producer the producer
     */
    protected ProducerStream(final ASupplier<Maybe<A>> producer) {
        this.producer = producer;
    }

    @Override
    protected Promise<Maybe<A>> produce() throws Throwable {
        return producer.get();
    }
}
