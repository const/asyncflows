/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

import static org.asyncflows.core.CoreFlows.aOutcome;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;
import static org.asyncflows.core.function.AsyncFunctionUtil.toAsyncFunction;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.asyncflows.core.annotations.ThreadSafe;
import org.asyncflows.core.context.Context;
import org.asyncflows.core.data.Subcription;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.function.AsyncFunctionUtil;
import org.asyncflows.core.trace.PromiseTrace;
import org.asyncflows.core.util.ExceptionUtil;
import org.asyncflows.core.vats.Vat;

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
@ThreadSafe
public final class Promise<T> {
    /**
     * The trace.
     */
    private final Object trace;
    /**
     * State of the promise. It is either {@code null}, {@link Cell} with listeners, or {@link Outcome}.
     */
    private final AtomicReference<Object> state = new AtomicReference<>();

    /**
     * Constructor of resolved promise from outcome.
     *
     * @param outcome the outcome
     */
    public Promise(final Outcome<T> outcome) {
        Objects.requireNonNull(outcome);
        state.set(outcome);
        this.trace = null;
    }

    /**
     * Constructor of unresolved promise.
     */
    public Promise() {
        this.trace = PromiseTrace.INSTANCE.recordTrace();
    }


    /**
     * Run result in this place, if action fails, wrap its result in failed promise.
     * This method never throws exception unless there is out ouf memory.
     *
     * @param action the action
     * @param <T>    the result type.
     * @return the promise
     */
    public static <T> Promise<T> get(final ASupplier<T> action) {
        try {
            final Promise<T> promise = action.get();
            //noinspection ReplaceNullCheck
            if (promise == null) {
                return new Promise<>(Outcome.failure(new NullPointerException("Action returned null: " + action.getClass().getName())));
            } else {
                return promise;
            }
        } catch (Throwable throwable) {
            return new Promise<>(Outcome.failure(throwable));
        }
    }

    /**
     * Transform method, that allows grouping some operations.
     *
     * @param body the body
     * @param <R>  the result type
     * @return the result
     */
    public <R> R transform(Function<Promise<T>, R> body) {
        Objects.requireNonNull(body);
        return body.apply(this);
    }

    /**
     * Add synchronous listener to promise. Note, this operation is low-level and it does not save a {@link Context},
     * and does not propagate it to listeners. The execution context is completely arbitrary.
     * The listener should be executed very fast and never block, prefer lock-free operations.
     * If there is a need for some processing, just create an event and send it to some executor.
     * Also, if you could have a long chain of promises, use {@link #listen(AResolver)} instead,
     * as this method could create a chain reaction of promise resolution.
     *
     * @param listener the listener.
     * @return this promise
     */
    @SuppressWarnings({"unchecked", "squid:S135"})
    public Promise<T> listenSync(final AResolver<? super T> listener) {
        Cell<AResolver<? super T>> listenerCell = null;
        while (true) {
            final Object currentState = state.get();
            if (currentState instanceof Outcome) {
                Outcome.notifyResolver(listener, (Outcome<T>) currentState);
                break;
            } else {
                Cell<AResolver<? super T>> next = (Cell<AResolver<? super T>>) currentState;
                if (listenerCell == null) {
                    listenerCell = new Cell<>(listener, next);
                } else {
                    listenerCell.setNext(next);
                }
                if (state.compareAndSet(next, listenerCell)) {
                    break;
                }
            }
        }
        return this;
    }

    /**
     * There are rare cases when control construct does not care about promise anymore,
     * so it is better to forget anyway in order to reduce memory usage and linked state.
     * Only listeners added with {@link #listenSync(AResolver)} could be forgotten.
     *
     * @param listener the listener
     * @return this promise
     */
    @SuppressWarnings({"unchecked", "squid:S135", "UnusedReturnValue"})
    public Promise<T> forget(final AResolver<? super T> listener) {
        while (true) {
            final Object currentState = state.get();
            if (currentState instanceof Cell) {
                final Cell<AResolver<? super T>> current = (Cell<AResolver<? super T>>) currentState;
                final Cell<AResolver<? super T>> modified = current.copyWithoutElement(listener);
                if (current == modified || state.compareAndSet(current, modified)) {
                    break;
                }
            } else {
                break;
            }
        }
        return this;
    }

    /**
     * Add asynchronous listener that uses default executor. Returns this promise.
     *
     * @param listener the listener.
     * @return this promise
     */
    public Promise<T> listen(final AResolver<? super T> listener) {
        return listen(Vat.current(), listener);
    }

    /**
     * Add asynchronous listener that uses specified executor. Returns this promise.
     *
     * @param vat      the executor.
     * @param listener the listener.
     * @return this promise
     */
    public Promise<T> listen(final Vat vat, final AResolver<? super T> listener) {
        final Context context = Context.current();
        return listenSync(o -> vat.execute(() -> {
            try (Subcription ignored = context.setContext()) {
                Outcome.notifyResolver(listener, o);
            }
        }));
    }

    /**
     * Get resolver. The resolver should be normally got only once and distributed to downstream services.
     *
     * @return the resolver
     */
    @SuppressWarnings({"unchecked", "squid:S3776", "squid:S135"})
    public AResolver<T> resolver() {
        return o -> {
            final Outcome<T> adjustedOutcome = o != null ? o : Outcome.failure(
                    new IllegalArgumentException("Notified with null outcome"));
            if (adjustedOutcome.isFailure() && trace != null) {
                PromiseTrace.INSTANCE.mergeTrace(adjustedOutcome.failure(), trace);
            }
            while (true) {
                final Object currentState = state.get();
                if (currentState instanceof Outcome) {
                    break;
                }
                if (!state.compareAndSet(currentState, adjustedOutcome)) {
                    continue;
                }
                if (currentState != null) {
                    final Cell<AResolver<? super T>> cell = (Cell<AResolver<? super T>>) currentState;
                    final AResolver<? super T>[] listeners = cell.toReversedArray(AResolver[]::new);
                    for (AResolver<? super T> listener : listeners) {
                        Outcome.notifyResolver(listener, o);
                    }
                    break;
                }
            }
        };
    }

    /**
     * Map outcome of promise in default execution context.
     *
     * @param function the mapper
     * @param <R>      the result type
     * @return the result promise
     */
    public <R> Promise<R> mapOutcome(final Function<Outcome<T>, R> function) {
        return flatMapOutcome(toAsyncFunction(function));
    }

    /**
     * Flat map outcome of promise in default execution context.
     *
     * @param mapper the mapper
     * @param <R>    the result type
     * @return the result promise
     */
    public <R> Promise<R> flatMapOutcome(final AFunction<Outcome<T>, R> mapper) {
        return flatMapOutcome(Vat.current(), mapper);
    }

    /**
     * Flat map outcome of promise.
     *
     * @param <R>    the result type
     * @param vat    the vat
     * @param mapper the mapper
     * @return the result promise
     */
    public <R> Promise<R> flatMapOutcome(final Vat vat, final AFunction<Outcome<T>, R> mapper) {
        final Outcome<T> currentOutcome = getOutcomeOrNull();
        if (currentOutcome == null) {
            final Promise<R> promise = new Promise<>();
            final AResolver<R> resolver = promise.resolver();
            listen(vat, o -> {
                try {
                    final Promise<R> result = mapper.apply(o);
                    if (result == null) {
                        Outcome.notifyFailure(resolver, new IllegalStateException("Body returned null"));
                    } else {
                        result.listenSync(resolver);
                    }
                } catch (Throwable e) {
                    Outcome.notifyFailure(resolver, e);
                }
            });
            return promise;
        } else {
            try {
                final Promise<R> promise = mapper.apply(currentOutcome);
                //noinspection ReplaceNullCheck
                if (promise != null) {
                    return promise;
                } else {
                    return new Promise<>(Outcome.failure(new IllegalStateException("Body returned null")));
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
        return flatMap(Vat.current(), mapper);
    }

    /**
     * Flat map successful outcome of promise. The failure is just passed through.
     *
     * @param vat    the vat
     * @param mapper the mapper
     * @param <R>    the result type
     * @return the promise for mapped result.
     */
    public <R> Promise<R> flatMap(final Vat vat, final AFunction<T, R> mapper) {
        return flatMapOutcome(
                vat, o -> o.isFailure() ? new Promise<>(Outcome.failure(o.failure())) : mapper.apply(o.value()));
    }

    /**
     * Map failure.
     *
     * @param action the synchronous action
     * @return the promise for action result
     */
    public Promise<T> mapFailure(final Function<Throwable, T> action) {
        return flatMapFailure(toAsyncFunction(action));
    }

    /**
     * Flat map failure.
     *
     * @param action the synchronous action
     * @return the promise for action result
     */
    public Promise<T> flatMapFailure(final AFunction<Throwable, T> action) {
        return flatMapFailure(Vat.current(), action);
    }


    /**
     * Flat map failure.
     *
     * @param vat    the vat
     * @param action the synchronous action
     * @return the promise for action result
     */
    public Promise<T> flatMapFailure(final Vat vat, final AFunction<Throwable, T> action) {
        return flatMapOutcome(vat, o -> {
            if (o.isFailure()) {
                return action.apply(o.failure());
            } else {
                return aOutcome(o);
            }
        });
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
        return flatMap(Vat.current(), t -> supplier.get());
    }

    /**
     * Get value after promise is finished.
     *
     * @param <R>      the result
     * @param vat      the executor
     * @param supplier a suppler
     * @return the promise that resolves when after current promise resolves and action is executed.
     */
    public <R> Promise<R> thenFlatGet(final Vat vat, final ASupplier<R> supplier) {
        return flatMap(vat, AsyncFunctionUtil.supplierToFunction(supplier));
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
    @SuppressWarnings("unchecked")
    public Outcome<T> getOutcomeOrNull() {
        final Object currentState = state.get();
        return currentState instanceof Outcome ? (Outcome<T>) currentState : null;
    }

    /**
     * @return the completable future, that is notified when promise is resolved.
     */
    public CompletableFuture<T> toCompletableFuture() {
        final Outcome<T> outcome = getOutcomeOrNull();
        if (outcome != null && outcome.isSuccess()) {
            return CompletableFuture.completedFuture(outcome.value());
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
    public <R> Promise<R> thenFailure(final Throwable failure) {
        final Throwable ex = failure != null ? failure : new IllegalArgumentException("Failure cannot be null");
        return flatMapOutcome(o -> {
            if (o.isFailure()) {
                ExceptionUtil.addSuppressed(ex, o.failure());
            }
            return new Promise<>(Outcome.failure(ex));
        });
    }

    /**
     * @return true if promise is unresolved yet
     */
    public boolean isUnresolved() {
        return getOutcomeOrNull() == null;
    }

    /**
     * @return the promise that resolves to outcome of this promise.
     */
    public Promise<Outcome<T>> toOutcomePromise() {
        return mapOutcome(Function.identity());
    }

    /**
     * Combine two promises.
     *
     * @param result the next promise
     * @param <R>    the result type
     * @return the promise that resolves when both this and next promise finishes. The operation fails
     * if either promise fails.
     */
    public <R> Promise<R> thenPromise(final Promise<R> result) {
        return thenFlatGet(promiseSupplier(result));
    }


    /**
     * Execute action after promise ignoring the result.
     * <ul>
     * <li>If action is successful, original outcome is used.</li>
     * <li>If action is failed and original was success, failure from action is used.</li>
     * <li>If action is failed and original was failure, original failure is used,
     * but failure from action is added to suppressed exception list.</li>
     * </ul>
     *
     * @param action the action
     * @param <I>    the ignored result type
     * @return the promise for result
     */
    public <I> Promise<T> finallyDo(ASupplier<I> action) {
        return flatMapOutcome(o -> get(action).flatMapOutcome(f -> {
            if (f.isSuccess()) {
                return new Promise<>(o);
            } else if (o.isSuccess()) {
                return new Promise<>(Outcome.failure(f.failure()));
            } else {
                ExceptionUtil.addSuppressed(o.failure(), f.failure());
                return new Promise<>(o);
            }
        }));
    }

    /**
     * The cell used in the listener list. It is designed to be kept in {@link AtomicReference},
     * so its operations are designed not to corrupt values while it is being removed.
     *
     * @param <E>
     */
    private static class Cell<E> {
        /**
         * The value.
         */
        private final E value;
        /**
         * The size.
         */
        private int size;
        /**
         * The next cell.
         */
        private Cell<E> next;

        /**
         * The constructor.
         *
         * @param value the cell value
         * @param next  the next cell (nullable)
         */
        private Cell(E value, Cell<E> next) {
            this.value = value;
            setNext(next);
        }

        /**
         * Set next cell.
         *
         * @param next the next cell
         */
        private void setNext(Cell<E> next) {
            this.next = next;
            this.size = next == null ? 1 : next.size + 1;
        }

        /**
         * Convert cells to reversed array.
         *
         * @param constructor the array constructor
         * @return the array of elements
         */
        private E[] toReversedArray(IntFunction<E[]> constructor) {
            final E[] array = constructor.apply(size);
            int count = size;
            for (Cell<E> c = this; c != null; c = c.next) {
                array[--count] = c.value;
            }
            return array;
        }

        /**
         * Return list that does not contain specified element. The tail after removed element is the same,
         * but new cells are created before that element.
         *
         * @param element the element
         * @return the value
         */
        private Cell<E> copyWithoutElement(E element) {
            Cell<E> cellWithElement = null;
            // find cell with element
            for (Cell<E> c = this; c != null; c = c.next) {
                if (c.value == element) {
                    cellWithElement = c;
                    break;
                }
            }
            if (cellWithElement == null) {
                return this;
            }
            if (cellWithElement == this) {
                return next;
            }

            Cell<E> newHead = null;
            Cell<E> previous = null;
            for (Cell<E> c = this; c != null; c = c.next) {
                if (c == cellWithElement) {
                    previous.next = c.next;
                    break;
                }
                final Cell<E> n = new Cell<>(c.value, null);
                n.size = c.size - 1;
                if (previous == null) {
                    newHead = n;
                } else {
                    previous.next = n;
                }
                previous = n;
            }
            return newHead;
        }
    }
}
