/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.core.function;

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;

import java.util.function.Function;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The utilities for teh functions.
 */
public final class AsyncFunctionUtil {

    /**
     * Private constructor for utility class.
     */
    private AsyncFunctionUtil() {
        // do nothing
    }

    /**
     * The convert synchronous function to asynchronous..
     *
     * @param function the function
     * @param <A>      the argument type
     * @param <R>      the result type
     * @return a supplier
     */
    public static <A, R> AFunction<A, R> toAsyncFunction(final Function<A, R> function) {
        return a -> aNow(() -> aValue(function.apply(a)));
    }

    /**
     * The supplier for the constant.
     *
     * @param value the value
     * @param <R>   the result type
     * @return a supplier
     */
    public static <R> ASupplier<R> constantSupplier(final R value) {
        return () -> aValue(value);
    }

    /**
     * The supplier for the promise.
     *
     * @param value the value
     * @param <R>   the result type
     * @return a supplier
     */
    public static <R> ASupplier<R> promiseSupplier(final Promise<R> value) {
        return () -> value != null ? value : aFailure(new IllegalArgumentException("Null is not allowed here"));
    }

    /**
     * The supplier that always return failure.
     *
     * @param failure the failure
     * @param <R>     the result type
     * @return a supplier
     */
    public static <R> ASupplier<R> failureSupplier(final Throwable failure) {
        return () -> aFailure(failure);
    }

    /**
     * Convert supplier to function that ignores its argument.
     *
     * @param supplier teh supplier
     * @param <A>      the argument type
     * @param <R>      the result type
     * @return the function
     */
    public static <A, R> AFunction<A, R> supplierToFunction(final ASupplier<R> supplier) {
        return t -> supplier.get();
    }


    /**
     * Create binary function from unary function that uses the second argument of binary function.
     *
     * @param function the function
     * @param <A>      the first argument type
     * @param <B>      the second argument type
     * @param <C>      the result type
     * @return the binary function
     */
    public static <A, B, C> AFunction2<A, B, C> useSecondArg(final AFunction<B, C> function) {
        return (a, b) -> function.apply(b);

    }

    /**
     * Create binary function from unary function that uses the first argument of binary function.
     *
     * @param function the function
     * @param <A>      the first argument type
     * @param <B>      the second argument type
     * @param <C>      the result type
     * @return the binary function
     */
    public static <A, B, C> AFunction2<A, B, C> useFirstArg(final AFunction<A, C> function) {
        return (a, b) -> function.apply(a);
    }

    /**
     * Invoke the function and always return promise, exceptions are not thrown.
     *
     * @param function the function
     * @param value    the value
     * @param <T>      the argument type
     * @param <R>      the result type
     * @return the invocation result
     */
    public static <T, R> Promise<R> evaluate(final AFunction<T, R> function, final T value) {
        return aNow(() -> function.apply(value));
    }

    /**
     * Return the supplier depending on the flag.
     *
     * @param value the value
     * @return the supplier
     */
    public static ASupplier<Boolean> booleanSupplier(final boolean value) {
        return value ? CoreFlows::aTrue : CoreFlows::aFalse;
    }

    /**
     * Mapper value to maybe for the specific type.
     *
     * @param <T> the type
     * @return the mapper
     */
    public static <T> AFunction<T, Maybe<T>> maybeMapper() {
        return CoreFlows::aMaybeValue;
    }

}
