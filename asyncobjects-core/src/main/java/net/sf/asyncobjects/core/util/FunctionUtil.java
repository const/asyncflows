package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;

/**
 * The additional utilities for functions.
 */
public final class FunctionUtil {

    /**
     * The private constructor for the utility class.
     */
    private FunctionUtil() {
    }

    /**
     * Uncurry function to tuple form.
     *
     * @param toZip the function to uncurry
     * @param <R>   the result type
     * @param <A>   the first argument type
     * @param <B>   the second argument type
     * @return the function
     */
    public static <R, A, B> AFunction<R, Tuple2<A, B>> uncurry2(final AFunction2<R, A, B> toZip) {
        return new AFunction<R, Tuple2<A, B>>() {
            @Override
            public Promise<R> apply(final Tuple2<A, B> value) throws Throwable {
                return toZip.apply(value.getValue1(), value.getValue2());
            }
        };
    }

    /**
     * Uncurry function to tuple form.
     *
     * @param toZip the function to uncurry
     * @param <R>   the result type
     * @param <A>   the first argument type
     * @param <B>   the second argument type
     * @param <C>   the third argument type
     * @return the function
     */
    public static <R, A, B, C> AFunction<R, Tuple3<A, B, C>> uncurry3(final AFunction3<R, A, B, C> toZip) {
        return new AFunction<R, Tuple3<A, B, C>>() {
            @Override
            public Promise<R> apply(final Tuple3<A, B, C> value) throws Throwable {
                return toZip.apply(value.getValue1(), value.getValue2(), value.getValue3());
            }
        };
    }


    /**
     * Build a two argument function from one argument function that. The resulting function uses the second
     * argument as input.
     *
     * @param function input function
     * @param <R>      the result type
     * @param <A>      the first argument type
     * @param <B>      the second argument type
     * @return the result function
     */
    public static <R, A, B> AFunction2<R, A, B> useUseSecondArg(final AFunction<R, B> function) {
        return new AFunction2<R, A, B>() {
            @Override
            public Promise<R> apply(final A value1, final B value2) throws Throwable {
                return function.apply(value2);
            }
        };
    }

}
