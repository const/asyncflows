package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.ProducerUtil;

import java.util.Arrays;
import java.util.Iterator;

/**
 * The stream factory.
 */
public final class Streams {
    /**
     * The private constructor for utility class.
     */
    private Streams() {
    }

    /**
     * Start building a stream from producer.
     *
     * @param producer the producer.
     * @param <A>      the stream element type
     * @return the stream builder for the stream.
     */
    public static <A> StreamBuilder<A> aForProducer(final ACallable<Maybe<A>> producer) {
        return new StreamBuilder<>(new ProducerStream<>(producer));
    }

    /**
     * Stream builder from iterable object.
     *
     * @param start the start of range
     * @param end   the end of range (not included)
     * @return the stream builder for the iterator.
     */
    public static StreamBuilder<Integer> aForRange(final int start, final int end) {
        return aForProducer(ProducerUtil.fromRange(start, end));
    }

    /**
     * Stream builder from iterable.
     *
     * @param iterable the iterable
     * @param <A>      the stream element type
     * @return the stream builder for the iterable.
     */
    public static <A> StreamBuilder<A> aForIterable(final Iterable<A> iterable) {
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
    public static <A> StreamBuilder<A> aForArray(final A... elements) {
        return aForIterable(Arrays.asList(elements));
    }

    /**
     * Stream builder from iterator.
     *
     * @param iterator the iterator
     * @param <A>      the stream element type
     * @return the stream builder for the range.
     */
    public static <A> StreamBuilder<A> aForIterator(final Iterator<A> iterator) {
        return aForProducer(ProducerUtil.fromIterator(iterator));
    }

    /**
     * Start building a stream.
     *
     * @param stream the stream to wrap
     * @param <T>    the stream element type
     * @return the stream builder
     */
    public static <T> StreamBuilder<T> aForStream(final AStream<T> stream) {
        return new StreamBuilder<>(stream);
    }
}
