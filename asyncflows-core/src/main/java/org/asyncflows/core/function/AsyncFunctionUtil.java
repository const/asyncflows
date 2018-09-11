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
 *
 */
public class AsyncFunctionUtil {
    public static <A, R> AFunction<A, R> toAsyncFunction(Function<A, R> function) {
        return a -> aNow(() -> aValue(function.apply(a)));
    }

    public static <R> ASupplier<R> constantSupplier(R value) {
        return () -> aValue(value);
    }

    public static <R> ASupplier<R> promiseSupplier(Promise<R> value) {
        return () -> value != null ? value : aFailure(new NullPointerException("Null is not allowed here"));
    }

    public static <R> ASupplier<R> failureSupplier(Throwable failure) {
        return () -> aFailure(failure);
    }

    public static <A, R> AFunction<A, R> supplierToFunction(ASupplier<R> supplier) {
        return t -> supplier.get();
    }

    public static <A, B, C> AFunction2<A, B, C> useSecondArg(AFunction<B, C> function) {
        return (a, b) -> function.apply(b);

    }

    public static <A, B, C> AFunction2<A, B, C> useFirstArg(AFunction<A, C> function) {
        return (a, b) -> function.apply(a);
    }

    public static <T, N> Promise<N> evaluate(AFunction<T, N> mapper, T value) {
        return aNow(() -> mapper.apply(value));
    }

    public static ASupplier<Boolean> booleanSupplier(boolean value) {
        return value ? CoreFlows::aTrue : CoreFlows::aFalse;
    }

    public static <T> AFunction<T, Maybe<T>> maybeMapper() {
        return CoreFlows::aMaybeValue;
    }

}
