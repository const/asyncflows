/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
     * @param start        the start of range
     * @param endExclusive the end of range (not included)
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
