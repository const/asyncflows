package org.asyncflows.core.function;

import org.asyncflows.core.AsyncControl;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;

import java.util.function.Function;

import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aNow;
import static org.asyncflows.core.AsyncControl.aValue;

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
        return value ? AsyncControl::aTrue : AsyncControl::aFalse;
    }

    public static <T> AFunction<T, Maybe<T>> maybeMapper() {
        return AsyncControl::aMaybeValue;
    }

}
