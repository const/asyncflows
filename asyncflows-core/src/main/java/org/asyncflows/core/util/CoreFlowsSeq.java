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

package org.asyncflows.core.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ARunner;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.vats.Vat;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static org.asyncflows.core.AsyncContext.withDefaultContext;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aOutcome;
import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * Utility class with sequential control utilities.
 */
public final class CoreFlowsSeq {

    /**
     * Private constructor for utility class.
     */
    private CoreFlowsSeq() {
        // do nothing
    }

    /**
     * Iterate a collection.
     *
     * @param iterable the iterable collection
     * @param body     the body that iterates over it. If body returns false, the cycle is aborted.
     * @param <T>      the element type
     * @return the void promise
     */
    public static <T> Promise<Void> aSeqForUnit(final Iterable<T> iterable, final AFunction<T, Boolean> body) {
        return aSeqForUnit(iterable.iterator(), body);
    }

    /**
     * Iterate using iterator
     *
     * @param iterator the iterator
     * @param body     the body that iterates over it. If body returns false, the cycle is aborted.
     * @param <T>      the element type
     * @return the void promise
     */
    public static <T> Promise<Void> aSeqForUnit(Iterator<T> iterator, AFunction<T, Boolean> body) {
        return aSeqWhile(() -> {
            if (!iterator.hasNext()) {
                return aFalse();
            }
            final T next = iterator.next();
            return body.apply(next);
        });
    }

    public static <T> Promise<Void> aSeqForUnit(Stream<T> stream, final AFunction<T, Boolean> body) {
        return aSeqForUnit(stream.iterator(), body);
    }


    /**
     * Iterate using iterator.
     *
     * @param iterator  the iterator
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector
     * @param <T>       the element type
     * @param <R>       the body result type
     * @param <I>       the collector intermediate type
     * @param <C>       the final type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aSeqForCollect(Iterator<T> iterator, AFunction<T, R> body, Collector<R, I, C> collector) {
        I accumulator = collector.supplier().get();
        return aSeqWhile(() -> {
            if (!iterator.hasNext()) {
                return aFalse();
            }
            final T next = iterator.next();

            return aNow(() -> body.apply(next)).flatMap(e -> {
                collector.accumulator().accept(accumulator, e);
                return aTrue();
            });
        }).thenGet(() -> collector.finisher().apply(accumulator));
    }

    /**
     * Iterate using collection.
     *
     * @param collection the collection
     * @param body       the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector
     * @param <T>        the element type
     * @param <R>        the body result type
     * @param <I>        the collector intermediate type
     * @param <C>        the final type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aSeqForCollect(Iterable<T> collection, AFunction<T, R> body, Collector<R, I, C> collector) {
        return aSeqForCollect(collection.iterator(), body, collector);
    }

    /**
     * Iterate using iterator
     *
     * @param stream the stream to use
     * @param body   the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector
     * @param <T>    the element type
     * @param <R>    the body result type
     * @param <I>    the collector intermediate type
     * @param <C>    the final type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aSeqForCollect(Stream<T> stream, AFunction<T, R> body, Collector<R, I, C> collector) {
        return aSeqForCollect(stream.iterator(), body, collector);
    }

    /**
     * Start building sequential execution action.
     *
     * @param action the action
     * @param <T>    the initial type
     * @return the result type.
     */
    public static <T> SeqBuilder<T> aSeq(ASupplier<T> action) {
        return withDefaultContext((runner, vat) -> new SeqBuilder<>(action, runner, vat));
    }

    /**
     * The loop while loop body returns true.
     *
     * @param loopBody loop body.
     * @return the result.
     */
    public static Promise<Void> aSeqWhile(ASupplier<Boolean> loopBody) {
        return aSeqUntilValue(() -> aNow(loopBody).flatMap(v -> v ? aMaybeEmpty() : aMaybeValue(null)));
    }

    /**
     * The loop until body return a value (non-empty {@link Maybe}.
     *
     * @param loopBody the loop body
     * @param <T>      the returned value.
     * @return the promise for value
     */
    public static <T> Promise<T> aSeqUntilValue(ASupplier<Maybe<T>> loopBody) {
        ASupplier<T> loop = () -> aResolver(new Consumer<AResolver<T>>() {
            private AResolver<T> resolver;

            @Override
            public void accept(AResolver<T> resolver) {
                this.resolver = resolver;
                iterate();
            }

            private void iterate() {
                aNow(loopBody).listen(o -> {
                    if (o.isSuccess()) {
                        Maybe<T> v = o.value();
                        if (v.isEmpty()) {
                            iterate();
                        } else {
                            notifySuccess(resolver, v.value());
                        }
                    } else {
                        notifyFailure(resolver, o.failure());
                    }
                });
            }
        });
        return withDefaultContext((r, e) -> r.run(loop));
    }


    /**
     * Builder for the sequence of operations. Note that class may call all bodies on the single vat turn,
     * if they resolve immediately. So amount of bodies should be limited, in order prevent stack overflow.
     *
     * @param <T> the current result type
     */
    public static final class SeqBuilder<T> {
        /**
         * The action to execute.
         */
        private final ASupplier<T> action;
        /**
         * Vat used by sec builder.
         */
        private final Vat vat;
        /**
         * Runner.
         */
        private final ARunner runner;

        /**
         * The constructor.
         *
         * @param action the action to start with
         * @param runner the runner
         * @param vat    the vat
         */
        private SeqBuilder(final ASupplier<T> action, ARunner runner, Vat vat) {
            this.action = action;
            this.runner = runner;
            this.vat = vat;
        }

        /**
         * Convert to function that ignores argument.
         *
         * @param nextAction the next action
         * @param <A>        the argument type
         * @param <N>        the result type
         * @return the function that ignores argument
         */
        private static <A, N> AFunction<A, N> toFunction(ASupplier<N> nextAction) {
            return t -> nextAction.get();
        }

        /**
         * @return finish the sequence
         */
        public Promise<T> finish() {
            return runner.run(action);
        }

        /**
         * Add next step to the sequence.
         *
         * @param mapper the mapper
         * @param <N>    the next type
         * @return the sequence builder with next step
         */
        public <N> SeqBuilder<N> map(final AFunction<T, N> mapper) {
            return new SeqBuilder<>(mapCallable(action, mapper), runner, vat);
        }

        private <N> ASupplier<N> mapCallable(ASupplier<T> action, AFunction<T, N> mapper) {
            final Vat vat = this.vat;
            return () -> aNow(action).flatMap(vat, mapper);
        }

        /**
         * Observe result of previous actions.
         *
         * @param listener the the listener to be notified
         * @return the out
         */
        public SeqBuilder<T> listen(AResolver<T> listener) {
            final Vat currentVat = vat;
            final ASupplier<T> currentAction = this.action;
            return new SeqBuilder<>(() -> aNow(currentAction).listen(currentVat, listener), runner, currentVat);
        }

        /**
         * Add next step to the sequence.
         *
         * @param mapper the mapper
         * @param <N>    the next type
         * @return the sequence builder with next step
         */
        public <N> Promise<N> mapLast(final AFunction<T, N> mapper) {
            return map(mapper).finish();
        }

        /**
         * Add next step to the sequence.
         *
         * @param nextAction the action that ignores the input argument
         * @param <N>        the next type
         * @return the sequence builder with next step
         */
        public <N> SeqBuilder<N> thenDo(final ASupplier<N> nextAction) {
            return map(toFunction(nextAction));
        }

        /**
         * Add next step to the sequence.
         *
         * @param nextAction the action that ignores the input argument
         * @param <N>        the next type
         * @return the sequence builder with next step
         */
        public <N> Promise<N> thenDoLast(final ASupplier<N> nextAction) {
            return thenDo(nextAction).finish();
        }

        /**
         * Handle exception thrown by the previous steps.
         *
         * @param catcher the action that handles exceptions
         * @return the builder
         */
        public SeqBuilder<T> failed(final AFunction<Throwable, T> catcher) {
            final Vat currentVat = vat;
            final ASupplier<T> currentAction = this.action;
            return new SeqBuilder<>(() -> aNow(currentAction).flatMapOutcome(currentVat, o -> {
                if (o.isSuccess()) {
                    return aOutcome(o);
                } else {
                    return catcher.apply(o.failure());
                }
            }), runner, currentVat);
        }


        /**
         * Add next step to the sequence.
         *
         * @param mapper the mapper
         * @return the sequence builder with next step
         */
        public Promise<T> failedLast(final AFunction<Throwable, T> mapper) {
            return failed(mapper).finish();
        }

        /**
         * Execute action in any case and the end of sequence operator.
         * The result promise is resolves to the original result after
         * the finally body is resolved. Unless the original body has
         * a success outcome, but finally body fails.
         *
         * @param finallyAction an action to execute
         * @return a promise for the sequence result
         */
        @SuppressWarnings("unchecked")
        public Promise<T> finallyDo(final ASupplier<Void> finallyAction) {
            final Vat currentVat = this.vat;
            return runner.run(() -> aNow(action).flatMapOutcome(currentVat, o -> aNow(finallyAction).flatMapOutcome(currentVat, o2 -> {
                if (o.isFailure()) {
                    if (o2.isFailure() && o2.failure() != o.failure()) {
                        o.failure().addSuppressed(o2.failure());
                    }
                    return aFailure(o.failure());
                } else {
                    if (o2.isFailure()) {
                        return aFailure(o2.failure());
                    } else {
                        return aValue(o.value());
                    }
                }
            })));
        }
    }
}
