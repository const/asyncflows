package net.sf.asyncobjects.core;

import net.sf.asyncobjects.core.data.Maybe;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;

/**
 * Core utilities for the functions.
 */
public final class CoreFunctionUtil {
    /**
     * The identity function.
     */
    private static final AFunction<Object, Object> IDENTITY = AsyncControl::aValue;
    /**
     * True callable.
     */
    private static final ACallable<Boolean> TRUE_CALLABLE = AsyncControl::aTrue;
    /**
     * The false callable.
     */
    private static final ACallable<Boolean> FALSE_CALLABLE = AsyncControl::aFalse;
    /**
     * The function that transfers everything to void.
     */
    private static final AFunction<Void, ?> VOID_MAPPER = value -> aVoid();
    /**
     * The function that transfers everything to maybe.
     */
    private static final AFunction<Maybe<Object>, Object> MAYBE_MAPPER = AsyncControl::aMaybeValue;


    /**
     * The function that transfers everything to maybe.
     */
    private static final AFunction<Object, Maybe<Object>> UN_MAYBE_MAPPER = value -> aValue(value.value());

    /**
     * Private constructor for utility class.
     */
    private CoreFunctionUtil() {
    }

    /**
     * Transform one callable to another.
     *
     * @param source the source
     * @param mapper the mapper
     * @param <I>    the input type
     * @param <O>    the output type
     * @return the callable
     */
    public static <I, O> ACallable<O> mapCallable(final ACallable<I> source, final AFunction<O, I> mapper) {
        return () -> aNow(source).map(mapper);
    }

    /**
     * Evaluate function. This is different from normal call only because it never
     * throws an exception, instead it returns a promise with failure.
     *
     * @param value    the input value
     * @param function the function
     * @param <I>      the input type
     * @param <O>      the result type
     * @return the result
     */
    public static <I, O> Promise<O> evaluate(final I value, final AFunction<O, I> function) {
        try {
            return function.apply(value);
        } catch (Throwable ex) {
            return aFailure(ex);
        }
    }

    /**
     * Chain two function calls.
     *
     * @param function1 the fist function.
     * @param function2 the second function.
     * @param <I>       the input type
     * @param <T>       the intermediate type
     * @param <O>       the output type
     * @return the combined funtion
     */
    public static <I, T, O> AFunction<O, I> chain(final AFunction<T, I> function1, final AFunction<O, T> function2) {
        return value -> evaluate(value, function1).map(function2);
    }

    /**
     * Constant value function for boolean.
     *
     * @param value a boolean value
     * @return the constant callable that always return the fixed value.
     */
    public static ACallable<Boolean> booleanCallable(final boolean value) {
        return value ? TRUE_CALLABLE : FALSE_CALLABLE;
    }

    /**
     * The function that transforms everything to void.
     *
     * @param <T> the value
     * @return the function
     */
    @SuppressWarnings("unchecked")
    public static <T> AFunction<Void, T> voidMapper() {
        return (AFunction<Void, T>) VOID_MAPPER;
    }

    /**
     * Create identity function.
     *
     * @param <I> the function type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    public static <I> AFunction<I, I> identity() {
        return (AFunction<I, I>) IDENTITY;
    }

    /**
     * The mapper to maybe value.
     *
     * @param <I> the function type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    public static <I> AFunction<Maybe<I>, I> maybeMapper() {
        return (AFunction<Maybe<I>, I>) (Object) MAYBE_MAPPER;
    }

    /**
     * The mapper from maybe to the value.
     *
     * @param <I> the function type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    public static <I> AFunction<I, Maybe<I>> unMaybeMapper() {
        return (AFunction<I, Maybe<I>>) (Object) UN_MAYBE_MAPPER;
    }

    /**
     * The callable that always return the same value.
     *
     * @param value the value to return
     * @param <T>   the value type
     * @return the callable instance
     */
    public static <T> ACallable<T> constantCallable(final T value) {
        return promiseCallable(aValue(value));
    }

    /**
     * The callable that always return the same promise.
     *
     * @param promise the promise to return
     * @param <T>     the value type
     * @return the callable instance
     */
    public static <T> ACallable<T> promiseCallable(final Promise<T> promise) {
        return () -> promise;
    }

    /**
     * Convert callable to function by discarding argument.
     *
     * @param callable the callable to use
     * @param <A>      the input type
     * @param <B>      the result type
     * @return the one argument function
     */
    public static <B, A> AFunction<B, A> discardArgument(final ACallable<B> callable) {
        return value -> aNow(callable);
    }

    /**
     * The callable that always fails with the specified failure.
     *
     * @param throwable the throwable
     * @param <A>       the type of callable
     * @return the value
     */
    public static <A> ACallable<A> failureCallable(final Throwable throwable) {
        return promiseCallable(AsyncControl.<A>aFailure(throwable));
    }
}
