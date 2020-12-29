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

package org.asyncflows.core.util;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;

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
import static org.asyncflows.core.CoreFlows.aResolver;
import static org.asyncflows.core.CoreFlows.aTrue;
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
     * Iterate using iterator.
     *
     * @param iterator the iterator
     * @param body     the body that iterates over it. If body returns false, the cycle is aborted.
     * @param <T>      the element type
     * @return the void promise
     */
    public static <T> Promise<Void> aSeqForUnit(final Iterator<T> iterator, final AFunction<T, Boolean> body) {
        return aSeqWhile(() -> {
            if (!iterator.hasNext()) {
                return aFalse();
            }
            final T next = iterator.next();
            return body.apply(next);
        });
    }

    /**
     * Iterate while stream is not finished or while body not return false.
     *
     * @param stream the stream
     * @param body   the body
     * @param <T>    the element type
     * @return the promise for iteration finished
     */
    public static <T> Promise<Void> aSeqForUnit(final Stream<T> stream, final AFunction<T, Boolean> body) {
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
    public static <T, R, I, C> Promise<C> aSeqForCollect(final Iterator<T> iterator, final AFunction<T, R> body,
                                                         final Collector<R, I, C> collector) {
        final I accumulator = collector.supplier().get();
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
     * @param collector  the collector
     * @param <T>        the element type
     * @param <R>        the body result type
     * @param <I>        the collector intermediate type
     * @param <C>        the final type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aSeqForCollect(final Iterable<T> collection, final AFunction<T, R> body,
                                                         final Collector<R, I, C> collector) {
        return aSeqForCollect(collection.iterator(), body, collector);
    }

    /**
     * Iterate using stream.
     *
     * @param stream    the stream to use
     * @param body      the body that iterates over it. If body returns false, the cycle is aborted.
     * @param collector the collector
     * @param <T>       the element type
     * @param <R>       the body result type
     * @param <I>       the collector intermediate type
     * @param <C>       the final type
     * @return the void promise
     */
    public static <T, R, I, C> Promise<C> aSeqForCollect(final Stream<T> stream, final AFunction<T, R> body,
                                                         final Collector<R, I, C> collector) {
        return aSeqForCollect(stream.iterator(), body, collector);
    }

    /**
     * Start building sequential execution action.
     *
     * @param action the action
     * @param <T>    the initial type
     * @return the result type.
     */
    public static <T> Promise<T> aSeq(final ASupplier<T> action) {
        return aNow(action);
    }

    /**
     * The loop while loop body returns true.
     *
     * @param loopBody loop body.
     * @return the result.
     */
    @SuppressWarnings("java:S5411")
    public static Promise<Void> aSeqWhile(final ASupplier<Boolean> loopBody) {
        return aSeqUntilValue(() -> aNow(loopBody).flatMap(v -> {
            if (v == null) {
                return aFailure(new NullPointerException("Null is not expected"));
            }
            return v ? aMaybeEmpty() : aMaybeValue(null);
        }));
    }

    /**
     * The loop until body return a value (non-empty {@link Maybe}.
     *
     * @param loopBody the loop body
     * @param <T>      the returned value.
     * @return the promise for value
     */
    @SuppressWarnings({"squid:S3776", "squid:S135"})
    public static <T> Promise<T> aSeqUntilValue(final ASupplier<Maybe<T>> loopBody) {
        final ASupplier<T> loop = () -> aResolver(new Consumer<AResolver<T>>() {
            private AResolver<T> resolver;

            @Override
            public void accept(final AResolver<T> resolver) {
                this.resolver = resolver;
                iterate();
            }

            private void iterate() {
                while (true) {
                    final Promise<Maybe<T>> result = aNow(loopBody);
                    final Outcome<Maybe<T>> outcome = result.getOutcome();
                    if (outcome == null) {
                        result.listen(o -> {
                            if (checkStep(o)) {
                                iterate();
                            }
                        });
                        break;
                    } else {
                        if (!checkStep(outcome)) {
                            break;
                        }
                    }
                }
            }

            private boolean checkStep(Outcome<Maybe<T>> o) {
                if (o.isSuccess()) {
                    Maybe<T> v = o.value();
                    if (v.isEmpty()) {
                        return true;
                    } else {
                        notifySuccess(resolver, v.value());
                        return false;
                    }
                } else {
                    notifyFailure(resolver, o.failure());
                    return false;
                }
            }
        });
        return withDefaultContext((r, e) -> r.run(loop));
    }
}
