package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;

/**
 * The base for simple producer stream that delegates to {@link ACallable}.
 *
 * @param <A> the stream element type
 */
public class ProducerStream<A> extends StreamBase<A> {
    /**
     * The producer for the values.
     */
    private final ACallable<Maybe<A>> producer;

    /**
     * The producer stream.
     *
     * @param producer the producer
     */
    protected ProducerStream(final ACallable<Maybe<A>> producer) {
        this.producer = producer;
    }

    @Override
    protected Promise<Maybe<A>> produce() throws Throwable {
        return producer.call();
    }
}
