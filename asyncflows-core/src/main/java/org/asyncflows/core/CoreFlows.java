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

package org.asyncflows.core;

import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AOneWayAction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.vats.Vat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.vats.Vats.defaultVat;

/**
 * Basic asynchronous control constructs.
 */
public final class CoreFlows {
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreFlows.class);
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
     * Private constructor for utility class.
     */
    private CoreFlows() {
        // do nothing
    }

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
    public static <T> Promise<T> aValue(final T value) {
        return new Promise<>(Outcome.success(value));
    }

    /**
     * Get promise with specified outcome.
     *
     * @param outcome the outcome
     * @param <T>     the type
     * @return the promise
     */
    public static <T> Promise<T> aOutcome(final Outcome<T> outcome) {
        return new Promise<>(outcome);
    }

    /**
     * Promise for for failure.
     *
     * @param throwable the problem
     * @param <T>       the expected type
     * @return the promise
     */
    public static <T> Promise<T> aFailure(final Throwable throwable) {
        return new Promise<>(Outcome.failure(throwable));
    }

    /**
     * Resolver block promise.
     *
     * @param resolverAction the action that takes resolver for just created promise.
     * @param <T>            the result type.
     * @return the promise.
     */
    public static <T> Promise<T> aResolver(final Consumer<AResolver<T>> resolverAction) {
        final Promise<T> promise = new Promise<>();
        final AResolver<T> resolver = promise.resolver();
        try {
            resolverAction.accept(resolver);
        } catch (Throwable e) {
            notifyFailure(resolver, e);
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
    public static <T> Promise<T> aNow(final ASupplier<T> action) {
        try {
            final Promise<T> promise = action.get();
            if (promise == null) {
                return aFailure(new NullPointerException("Action returned null: " + action.getClass().getName()));
            } else {
                return promise;
            }
        } catch (Throwable throwable) {
            return aFailure(throwable);
        }
    }

    /**
     * Send event to vat.
     *
     * @param vat    the vat
     * @param action the action
     */
    public static void aSend(final Vat vat, final Runnable action) {
        vat.execute(action);
    }

    /**
     * Send event to the default vat.
     *
     * @param action the action
     */
    public static void aSend(final Runnable action) {
        defaultVat().execute(action);
    }

    /**
     * Send one-way action to vat.
     *
     * @param vat    the vat
     * @param action the action
     */
    public static void aOneWay(final Vat vat, final AOneWayAction action) {
        vat.execute(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                LOGGER.error("One-way action failed", throwable);
            }
        });
    }

    /**
     * Send one-way action to vat.
     *
     * @param executor the vat
     * @param action   the action
     */
    public static Promise<Void> aExecutorAction(final Executor executor, final AOneWayAction action) {
        return aResolver(r -> {
            executor.execute(() -> {
                try {
                    action.run();
                    notifySuccess(r, null);
                } catch (Throwable throwable) {
                    notifyFailure(r, throwable);
                }
            });
        });
    }

    /**
     * Send even to the default vat.
     *
     * @param action the action
     */
    public static void aOneWay(final AOneWayAction action) {
        aOneWay(defaultVat(), action);
    }

    /**
     * Convert generic completion stage to promise. It handles {@link CompletableFuture}
     * in the optimized way.
     *
     * @param stage the stage
     * @param <T>   the result type
     * @return the result
     */
    public static <T> Promise<T> aStageResult(final CompletionStage<T> stage) {
        if (stage == null) {
            return aFailure(new IllegalArgumentException("Action should return non-null"));
        }
        if (stage instanceof CompletableFuture) {
            final CompletableFuture<T> future = (CompletableFuture<T>) stage;
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
     * Execute action later on specific vat.
     *
     * @param action the action
     * @param vat    the vat
     * @param <T>    the result type
     * @return the promise for result
     */
    public static <T> Promise<T> aLater(final Vat vat, final ASupplier<T> action) {
        return aResolver(r -> vat.execute(() -> {
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
    public static <T> Promise<T> aLater(final ASupplier<T> action) {
        return aLater(defaultVat(), action);
    }

    /**
     * Operation that never completes. The promise will accumulate listeners that listenSync to it.
     * So do not share it between contexts.
     *
     * @param <T> the supposed result
     * @return the promise
     */
    public static <T> Promise<T> aNever() {
        final Promise<T> promise = new Promise<>();
        promise.resolver();
        return promise;
    }
}
