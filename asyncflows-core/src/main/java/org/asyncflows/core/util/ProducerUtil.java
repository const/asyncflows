/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

package org.asyncflows.core.util;

import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.failureSupplier;

import java.util.Iterator;

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.ASupplier;

/**
 * Utilities for creating producers.
 */
public final class ProducerUtil {
    /**
     * The wrapper into {@link Maybe}.
     */
    private static final AFunction<Object, Maybe<Object>> OPTIONAL_WRAPPER = CoreFlows::aMaybeValue;

    /**
     * The private constructor for utility class.
     */
    private ProducerUtil() {
    }

    /**
     * Create producer from collection.
     *
     * @param iterable the iterable
     * @param <T>      the collection element type
     * @return a created producer
     */
    public static <T> ASupplier<Maybe<T>> fromIterable(final Iterable<T> iterable) {
        try {
            return fromIterator(iterable.iterator());
        } catch (Throwable t) {
            return failureSupplier(t);
        }
    }

    /**
     * Create producer from iterator.
     *
     * @param iterator the iterator
     * @param <T>      the collection element type
     * @return a created producer
     */
    public static <T> ASupplier<Maybe<T>> fromIterator(final Iterator<T> iterator) {
        return () -> {
            if (iterator.hasNext()) {
                return aMaybeValue(iterator.next());
            } else {
                return aMaybeEmpty();
            }
        };
    }

    /**
     * Construct a callable that iterates over range of integers. It starts from {@code start} value
     * and iterates util end is reached.
     *
     * @param start        the iteration start
     * @param endExclusive the iteration end
     * @return the iteration
     */
    public static ASupplier<Maybe<Integer>> fromRange(final int start, final int endExclusive) {
        return new ASupplier<Maybe<Integer>>() {
            private int current = start;

            @Override
            public Promise<Maybe<Integer>> get() throws Exception {
                if (current < endExclusive) {
                    return aMaybeValue(current++);
                } else {
                    return aMaybeEmpty();
                }
            }
        };
    }

    /**
     * Map producer to other value.
     *
     * @param producer the producer to map
     * @param mapper   the mapper
     * @param <B>      the result producer type
     * @param <A>      the source producer type
     * @return the new producer that first get value from original and then maps to using mapper
     */
    public static <B, A> ASupplier<Maybe<B>> mapProducer(final ASupplier<Maybe<A>> producer,
                                                         final AFunction<A, B> mapper) {
        final AFunction<Maybe<A>, Maybe<B>> optionalMapper = toProducerMapper(mapper);
        return () -> producer.get().flatMap(optionalMapper);
    }

    /**
     * Create producer mapper from normal mapper.
     *
     * @param mapper the mapper
     * @param <B>    the input type
     * @param <A>    the output type
     * @return the producer mapper
     */
    public static <B, A> AFunction<Maybe<A>, Maybe<B>> toProducerMapper(final AFunction<A, B> mapper) {
        return value -> {
            if (value.isEmpty()) {
                return aMaybeEmpty();
            } else {
                return mapper.apply(value.of()).flatMap(ProducerUtil.optionalValueWrapper());
            }
        };
    }

    /**
     * Provider a function that wraps a value into an option.
     *
     * @param <T> the option
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public static <T> AFunction<T, Maybe<T>> optionalValueWrapper() {
        return (AFunction<T, Maybe<T>>) (AFunction) OPTIONAL_WRAPPER;
    }
}
