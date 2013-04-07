package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;

import java.util.Iterator;

import static net.sf.asyncobjects.core.AsyncControl.aSuccess;

/**
 * Utilities for creating producers.
 */
public final class ProducerUtil {
    /**
     * Empty value.
     */
    private static final Promise<OptionalValue<Object>> EMPTY_VALUE = aSuccess(OptionalValue.empty());
    /**
     * The wrapper into {@link OptionalValue}.
     */
    private static final AFunction<OptionalValue<Object>, Object> OPTIONAL_WRAPPER
            = new AFunction<OptionalValue<Object>, Object>() {
        @Override
        public Promise<OptionalValue<Object>> apply(final Object value) throws Throwable {
            return aSuccess(OptionalValue.value(value));
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
     * @param <T>      the collection
     * @return a created producer
     */
    public static <T> ACallable<OptionalValue<T>> fromIterable(final Iterable<T> iterable) {
        final Iterator<T> iterator = iterable.iterator();
        return new ACallable<OptionalValue<T>>() {
            @Override
            public Promise<OptionalValue<T>> call() throws Throwable {
                if (iterator.hasNext()) {
                    return aSuccess(OptionalValue.value(iterator.next()));
                } else {
                    return aEmptyOption();
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
    public static ACallable<OptionalValue<Integer>> fromRange(final int start, final int end) {
        return new ACallable<OptionalValue<Integer>>() {
            private int current = start;

            @Override
            public Promise<OptionalValue<Integer>> call() throws Throwable {
                if (current < end) {
                    return aSuccess(OptionalValue.value(current++));
                } else {
                    return aEmptyOption();
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
    public static <B, A> ACallable<OptionalValue<B>> mapProducer(final ACallable<OptionalValue<A>> producer,
                                                                 final AFunction<B, A> mapper) {
        final AFunction<OptionalValue<B>, OptionalValue<A>> optionalMapper =
                new AFunction<OptionalValue<B>, OptionalValue<A>>() {
                    @Override
                    public Promise<OptionalValue<B>> apply(final OptionalValue<A> value) throws Throwable {
                        if (value.isEmpty()) {
                            return aEmptyOption();
                        } else {
                            return mapper.apply(value.value()).map(ProducerUtil.<B>optionalValueWrapper());
                        }
                    }
                };
        return new ACallable<OptionalValue<B>>() {
            @Override
            public Promise<OptionalValue<B>> call() throws Throwable {
                return producer.call().map(optionalMapper);
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
    public static <T> AFunction<OptionalValue<T>, T> optionalValueWrapper() {
        return (AFunction<OptionalValue<T>, T>) (AFunction) OPTIONAL_WRAPPER;
    }

    /**
     * Return empty option value.
     *
     * @param <T> the value type
     * @return the resolved promise for empty value
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<OptionalValue<T>> aEmptyOption() {
        return (Promise<OptionalValue<T>>) (Promise) EMPTY_VALUE;
    }
}
