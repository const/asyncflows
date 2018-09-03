package org.asyncflows.core;

import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.function.AsyncFunctionUtil;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.asyncflows.core.AsyncControl.aOutcome;
import static org.asyncflows.core.vats.Vats.defaultVat;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.function.AsyncFunctionUtil.toAsyncFunction;

/**
 * Promise class, it is different from future that it is final, does not support
 * modification, and forbids post-resolutions modifications.
 * <p>
 * If sync methods are used, they should be very short and care about
 * own concurrency or they should dispatch job internally. The methods
 * that provide extensive processing are strongly discouraged.
 *
 * @param <T> the parameter types.
 */
public final class Promise<T> {
    /**
     * Lock for promise state.
     */
    private final Object lock = new Object();
    /**
     * The list of listeners.
     */
    private LinkedList<AResolver<? super T>> listeners;
    /**
     * The outcome (null if not resolved).
     */
    private Outcome<T> outcome;
    /**
     * True if resolver has been already acquired.
     */
    private boolean resolverAcquired;

    /**
     * Constructor of resolved promise from outcome.
     *
     * @param outcome the outcome
     */
    public Promise(Outcome<T> outcome) {
        Objects.requireNonNull(outcome);
        this.resolverAcquired = true;
        this.outcome = outcome;
    }

    /**
     * Constructor of unresolved promise.
     */
    public Promise() {
        this.listeners = new LinkedList<>();
    }


    /**
     * Add synchronous listener to promise.
     *
     * @param listener the listener.
     */
    public Promise<T> listenSync(AResolver<? super T> listener) {
        final Outcome<T> o;
        synchronized (lock) {
            if (outcome != null) {
                o = outcome;
            } else {
                listeners.add(listener);
                return this;
            }
        }
        Outcome.notifyResolver(listener, o);
        return this;
    }

    /**
     * Add asynchronous listener that uses default executor. Returns this promise.
     *
     * @param listener the listener.
     */
    public Promise<T> listen(AResolver<? super T> listener) {
        return listen(listener, defaultVat());
    }

    /**
     * Add asynchronous listener that uses specified executor. Returns this promise.
     *
     * @param listener the listener.
     * @param executor the executor.
     */
    public Promise<T> listen(AResolver<? super T> listener, Executor executor) {
        return listenSync(o -> executor.execute(() -> Outcome.notifyResolver(listener, o)));
    }

    /**
     * Get resolver. To ensure that consistency in resolution process, the resolver could be got only once.
     *
     * @return the resolver
     */
    public AResolver<T> resolver() {
        synchronized (lock) {
            if (resolverAcquired) {
                throw new IllegalStateException("Resolver is already acquired");
            }
            resolverAcquired = true;
            return o -> {
                final LinkedList<AResolver<? super T>> l;
                final Outcome<T> adjustedOutcome = o != null ? o : Outcome.failure(new NullPointerException("Notified with null outcome"));
                synchronized (lock) {
                    if (outcome == null) {
                        l = listeners;
                        listeners = null;
                        outcome = adjustedOutcome;
                    } else {
                        return;
                    }
                }
                for (AResolver<? super T> resolver : l) {
                    Outcome.notifyResolver(resolver, o);
                }
            };
        }
    }

    /**
     * Map outcome of promise in default execution context.
     *
     * @param function the mapper
     * @param <R>      the result type
     * @return the result promise
     */
    public <R> Promise<R> mapOutcome(Function<Outcome<T>, R> function) {
        return flatMapOutcome(toAsyncFunction(function));
    }

    /**
     * Flat map outcome of promise in default execution context.
     *
     * @param mapper the mapper
     * @param <R>    the result type
     * @return the result promise
     */
    public <R> Promise<R> flatMapOutcome(AFunction<Outcome<T>, R> mapper) {
        return flatMapOutcome(mapper, defaultVat());
    }

    /**
     * Flat map outcome of promise.
     *
     * @param mapper   the mapper
     * @param executor the executor
     * @param <R>      the result type
     * @return the result promise
     */
    public <R> Promise<R> flatMapOutcome(AFunction<Outcome<T>, R> mapper, Executor executor) {
        Outcome<T> outcome = getOutcome();
        if (outcome == null) {
            Promise<R> promise = new Promise<>();
            AResolver<R> resolver = promise.resolver();
            listen(o -> {
                try {
                    Promise<R> result = mapper.apply(o);
                    if (result == null) {
                        Outcome.notifyFailure(resolver, new NullPointerException("Body returned null"));
                    } else {
                        result.listenSync(resolver);
                    }
                } catch (Throwable e) {
                    Outcome.notifyFailure(resolver, e);
                }
            }, executor);
            return promise;
        } else {
            try {
                Promise<R> promise = mapper.apply(outcome);
                if (promise != null) {
                    return promise;
                } else {
                    return new Promise<>(Outcome.failure(new NullPointerException("Body returned null")));
                }
            } catch (Throwable throwable) {
                return new Promise<>(Outcome.failure(throwable));
            }
        }
    }

    /**
     * Map successful outcome of promise. The failure is just passed through.
     *
     * @param mapper the mapper
     * @param <R>    the result type
     * @return the promise for mapped result.
     */
    public <R> Promise<R> map(final Function<T, R> mapper) {
        return flatMap(toAsyncFunction(mapper));
    }

    /**
     * Flat map successful outcome of promise. The failure is just passed through.
     *
     * @param mapper the mapper
     * @param <R>    the result type
     * @return the promise for mapped result.
     */
    public <R> Promise<R> flatMap(final AFunction<T, R> mapper) {
        return flatMap(mapper, defaultVat());
    }


    /**
     * Flat map successful outcome of promise. The failure is just passed through.
     *
     * @param mapper   the mapper
     * @param executor the executor
     * @param <R>      the result type
     * @return the promise for mapped result.
     */
    public <R> Promise<R> flatMap(final AFunction<T, R> mapper, final Executor executor) {
        return flatMapOutcome(
                o -> o.isFailure() ? new Promise<>(Outcome.failure(o.failure())) : mapper.apply(o.value()), executor);
    }

    public Promise<T> mapFailure(final Function<Throwable, T> action) {
        return flatMapFailure(toAsyncFunction(action));
    }


    public Promise<T> flatMapFailure(final AFunction<Throwable, T> action) {
        return flatMapFailure(action, defaultVat());
    }


    public Promise<T> flatMapFailure(final AFunction<Throwable, T> action, Executor executor) {
        return flatMapOutcome(o -> {
            if (o.isFailure()) {
                return action.apply(o.failure());
            } else {
                return aOutcome(o);
            }
        }, executor);
    }

    /**
     * After promise resolves, just finish specified value.
     *
     * @param value the value to return
     * @param <R>   the result type
     * @return the promise
     */
    public <R> Promise<R> thenValue(final R value) {
        return map(t -> value);
    }

    /**
     * After promise resolves, just get specified value.
     *
     * @param supplier the the supplier for value
     * @param <R>      the result type
     * @return the promise
     */
    public <R> Promise<R> thenGet(final Supplier<R> supplier) {
        return map(t -> supplier.get());
    }

    /**
     * Get value after promise is finished.
     *
     * @param supplier a suppler
     * @param <R>      the result
     * @return the promise that resolves when after current promise resolves and action is executed.
     */
    public <R> Promise<R> thenFlatGet(final ASupplier<R> supplier) {
        return flatMap(t -> supplier.get(), defaultVat());
    }

    /**
     * Get value after promise is finished.
     *
     * @param supplier a suppler
     * @param executor the executor
     * @param <R>      the result
     * @return the promise that resolves when after current promise resolves and action is executed.
     */
    public <R> Promise<R> thenFlatGet(final ASupplier<R> supplier, final Executor executor) {
        return flatMap(AsyncFunctionUtil.supplierToFunction(supplier), executor);
    }

    /**
     * @return the void promise that resolves when promise resolves
     */
    public Promise<Void> toVoid() {
        return thenValue(null);
    }

    /**
     * @return the current outcome of promise.
     */
    public Outcome<T> getOutcome() {
        synchronized (lock) {
            return outcome;
        }
    }

    /**
     * @return the completable future, that is notified when promise is resolved.
     */
    public CompletableFuture<T> toCompletableFuture() {
        synchronized (lock) {
            if (outcome.isSuccess()) {
                return CompletableFuture.completedFuture(outcome.value());
            }
        }
        final CompletableFuture<T> future = new CompletableFuture<>();
        listenSync(o -> {
            if (o.isFailure()) {
                future.completeExceptionally(o.failure());
            } else {
                future.complete(o.value());
            }
        });
        return future;
    }

    /**
     * Create promise that fails with the specified promise.
     *
     * @param failure the failure
     * @param <R>     the result type
     * @return promise that fails after this promise resolves in some way.
     */
    public <R> Promise<R> thenFailure(Throwable failure) {
        return flatMapOutcome(o -> {
            Throwable ex = failure;
            if (ex == null) {
                ex = new NullPointerException("Failure cannot be null");
            }
            if (o.isFailure()) {
                ex.addSuppressed(o.failure());
            }
            return new Promise<>(Outcome.failure(ex));
        });
    }

    public boolean isUnresolved() {
        return getOutcome() == null;
    }

    public Promise<Outcome<T>> toOutcomePromise() {
        return mapOutcome(Function.identity());
    }

    public <T> Promise<T> thenPromise(Promise<T> result) {
        return thenFlatGet(promiseSupplier(result));
    }
}