package org.asyncflows.core;

import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.asyncflows.core.vats.Vats.defaultVat;

/**
 * Basic asynchronous control constructs.
 */
public class AsyncControl {
    /**
     * The constant promise NULL.
     */
    private static final Promise<Void> NULL = aValue(null);
    /**
     * The constant promise for true.
     */
    private static final Promise<Boolean> TRUE = aValue(true);
    /**
     * The constant promise for false.
     */
    private static final Promise<Boolean> FALSE = aValue(false);
    /**
     * Empty value.
     */
    private static final Promise<Maybe<Object>> EMPTY_VALUE = aValue(Maybe.empty());

    /**
     * @return the promise for void value.
     */
    public static Promise<Void> aVoid() {
        return NULL;
    }

    /**
     * Promise for null value of specified type.
     *
     * @param <T> the type
     * @return the promise.
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<T> aNull() {
        return (Promise<T>) (Object) NULL;
    }

    /**
     * @return the promise for true value.
     */
    public static Promise<Boolean> aTrue() {
        return TRUE;
    }

    /**
     * @return promise for false value.
     */
    public static Promise<Boolean> aFalse() {
        return FALSE;
    }

    /**
     * Return a promise basing on boolean value.
     *
     * @param value the boolean value
     * @return the corresponding promise
     */
    public static Promise<Boolean> aBoolean(final boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * Return empty option value.
     *
     * @param <T> the value type
     * @return the resolved promise for empty value
     */
    @SuppressWarnings("unchecked")
    public static <T> Promise<Maybe<T>> aMaybeEmpty() {
        return (Promise<Maybe<T>>) (Promise) EMPTY_VALUE;
    }

    /**
     * The maybe with value.
     *
     * @param value the value
     * @param <T>   the type
     * @return the maybe with value
     */
    public static <T> Promise<Maybe<T>> aMaybeValue(final T value) {
        return aValue(Maybe.value(value));
    }

    /**
     * Promise for the specified value. The result could be used as constant if value is immutable.
     *
     * @param value the value.
     * @param <T>   the type
     * @return a value
     */
    public static <T> Promise<T> aValue(T value) {
        return new Promise<>(Outcome.success(value));
    }

    public static <T> Promise<T> aOutcome(Outcome<T> outcome) {
        return new Promise<>(outcome);
    }

    /**
     * Promise for for failure.
     *
     * @param throwable the problem
     * @param <T>       the expected type
     * @return the promise
     */
    public static <T> Promise<T> aFailure(Throwable throwable) {
        return new Promise<>(Outcome.failure(throwable));
    }

    /**
     * Resolver block promise.
     *
     * @param resolverAction the action that takes resolver for just created promise.
     * @param <T>            the result type.
     * @return the promise.
     */
    public static <T> Promise<T> aResolver(Consumer<AResolver<T>> resolverAction) {
        final Promise<T> promise = new Promise<>();
        AResolver<T> resolver = promise.resolver();
        try {
            resolverAction.accept(resolver);
        } catch (Throwable e) {
            Outcome.notifyFailure(resolver, e);
        }
        return promise;
    }

    /**
     * Run result in this place, if action fails, wrap its result in failed promise.
     * This method never throws exception unless there is out ouf memory.
     *
     * @param action the action
     * @param <T>    the result type.
     * @return the promise
     */
    public static <T> Promise<T> aNow(ASupplier<T> action) {
        try {
            return action.get();
        } catch (Throwable throwable) {
            return aFailure(throwable);
        }
    }

    public static void aSend(Runnable action, Executor executor) {
        executor.execute(action);
    }

    public static void aSend(Runnable action) {
        defaultVat().execute(action);
    }

    /**
     * Convert generic completion stage to promise. It handles {@link Promise} and {@link CompletableFuture}
     * in the optimized way.
     *
     * @param stage the stage
     * @param <T>   the result type
     * @return the result
     */
    public static <T> Promise<T> aStageResult(CompletionStage<T> stage) {
        if (stage == null) {
            return aFailure(new NullPointerException("Action should return non-null"));

        }
        if (stage instanceof CompletableFuture) {
            CompletableFuture<T> future = (CompletableFuture<T>) stage;
            if (future.isDone()) {
                try {
                    return aValue(future.get());
                } catch (ExecutionException e) {
                    return aFailure(e.getCause());
                } catch (Throwable e) {
                    return aFailure(e);
                }
            }
        }
        return aResolver(r -> {
            stage.whenComplete((v, p) -> Outcome.notifyResolver(r, Outcome.of(v, p)));
        });
    }


    /**
     * Execute action later on specific executor.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the promise for result
     */
    public static <T> Promise<T> aLater(ASupplier<T> action, Executor executor) {
        return aResolver(r -> executor.execute(() -> {
            aNow(action).listenSync(r);
        }));
    }

    /**
     * Execute action later.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the promise for result
     */
    public static <T> Promise<T> aLater(ASupplier<T> action) {
        return aLater(action, defaultVat());
    }

    /**
     * Operation that never completes. The promise will accumulate listeners that listenSync to it.
     * So do not share it between contexts.
     *
     * @param <T> the supposed result
     * @return the promise
     */
    public static <T> Promise<T> aNever() {
        Promise<T> promise = new Promise<>();
        promise.resolver();
        return promise;
    }
}
