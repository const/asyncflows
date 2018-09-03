package org.asyncflows.core.streams;

import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.ProducerUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * The stream factory.
 */
public final class AsyncStreams {
    // TODO add support for JDK8 stream API
    /**
     * The private constructor for utility class.
     */
    private AsyncStreams() {
    }

    /**
     * Start building a stream from producer.
     *
     * @param producer the producer.
     * @param <A>      the stream element type
     * @return the stream builder for the stream.
     */
    public static <A> PullStreamBuilder<A> aForProducer(final ASupplier<Maybe<A>> producer) {
        return new PullStreamBuilder<>(new ProducerStream<>(producer));
    }

    /**
     * Stream builder from iterable object.
     *
     * @param start the start of range
     * @param endExclusive   the end of range (not included)
     * @return the stream builder for the iterator.
     */
    public static PullStreamBuilder<Integer> aForRange(final int start, final int endExclusive) {
        return aForProducer(ProducerUtil.fromRange(start, endExclusive));
    }

    /**
     * Stream builder from iterable.
     *
     * @param iterable the iterable
     * @param <A>      the stream element type
     * @return the stream builder for the iterable.
     */
    public static <A> PullStreamBuilder<A> aForIterable(final Iterable<A> iterable) {
        return aForProducer(ProducerUtil.fromIterable(iterable));
    }

    /**
     * Stream builder from array of elements.
     *
     * @param elements the iterable
     * @param <A>      the stream element type
     * @return the stream builder for the array.
     */
    @SafeVarargs
    public static <A> PullStreamBuilder<A> aForArray(final A... elements) {
        return aForIterable(Arrays.asList(elements));
    }

    /**
     * Stream builder from iterator.
     *
     * @param iterator the iterator
     * @param <A>      the stream element type
     * @return the stream builder for the range.
     */
    public static <A> PullStreamBuilder<A> aForIterator(final Iterator<A> iterator) {
        return aForProducer(ProducerUtil.fromIterator(iterator));
    }

    /**
     * Start building a stream.
     *
     * @param stream the stream to wrap
     * @param <T>    the stream element type
     * @return the stream builder
     */
    public static <T> PullStreamBuilder<T> aForStream(final AStream<T> stream) {
        return new PullStreamBuilder<>(stream);
    }


    /**
     * Start building a stream.
     *
     * @param stream the stream to wrap
     * @param <T>    the stream element type
     * @return the stream builder
     */
    public static <T> PullStreamBuilder<T> aForJavaStream(final Stream<T> stream) {
        return aForIterator(stream.iterator());
    }
}
