package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;

import java.util.Iterator;

import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.CoreFunctionUtil.failureCallable;

/**
 * Utilities for creating producers.
 */
public final class ProducerUtil {
    /**
     * The wrapper into {@link net.sf.asyncobjects.core.data.Maybe}.
     */
    private static final AFunction<Maybe<Object>, Object> OPTIONAL_WRAPPER
            = new AFunction<Maybe<Object>, Object>() {
        @Override
        public Promise<Maybe<Object>> apply(final Object value) throws Throwable {
            return aMaybeValue(value);
        }
    };

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
    public static <T> ACallable<Maybe<T>> fromIterable(final Iterable<T> iterable) {
        try {
            return fromIterator(iterable.iterator());
        } catch (Throwable t) {
            return failureCallable(t);
        }
    }

    /**
     * Create producer from iterator.
     *
     * @param iterator the iterator
     * @param <T>      the collection element type
     * @return a created producer
     */
    public static <T> ACallable<Maybe<T>> fromIterator(final Iterator<T> iterator) {
        return new ACallable<Maybe<T>>() {
            @Override
            public Promise<Maybe<T>> call() throws Throwable {
                if (iterator.hasNext()) {
                    return aMaybeValue(iterator.next());
                } else {
                    return aMaybeEmpty();
                }
            }
        };
    }

    /**
     * Construct a callable that iterates over range of integers. It starts from {@code start} value
     * and iterates util end is reached.
     *
     * @param start the iteration start
     * @param end   the iteration end
     * @return the iteration
     */
    public static ACallable<Maybe<Integer>> fromRange(final int start, final int end) {
        return new ACallable<Maybe<Integer>>() {
            private int current = start;

            @Override
            public Promise<Maybe<Integer>> call() throws Throwable {
                if (current <= end) {
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
    public static <B, A> ACallable<Maybe<B>> mapProducer(final ACallable<Maybe<A>> producer,
                                                         final AFunction<B, A> mapper) {
        final AFunction<Maybe<B>, Maybe<A>> optionalMapper = toProducerMapper(mapper);
        return new ACallable<Maybe<B>>() {
            @Override
            public Promise<Maybe<B>> call() throws Throwable {
                return producer.call().map(optionalMapper);
            }
        };
    }

    /**
     * Create producer mapper from normal mapper.
     *
     * @param mapper the mapper
     * @param <B>    the input type
     * @param <A>    the output type
     * @return the producer mapper
     */
    public static <B, A> AFunction<Maybe<B>, Maybe<A>> toProducerMapper(final AFunction<B, A> mapper) {
        return new AFunction<Maybe<B>, Maybe<A>>() {
            @Override
            public Promise<Maybe<B>> apply(final Maybe<A> value) throws Throwable {
                if (value.isEmpty()) {
                    return aMaybeEmpty();
                } else {
                    return mapper.apply(value.value()).map(ProducerUtil.<B>optionalValueWrapper());
                }
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
    public static <T> AFunction<Maybe<T>, T> optionalValueWrapper() {
        return (AFunction<Maybe<T>, T>) (AFunction) OPTIONAL_WRAPPER;
    }
}
