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

package org.asyncflows.tracker;


import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AFunction2;
import org.asyncflows.core.function.AFunction3;
import org.asyncflows.core.function.AFunction4;
import org.asyncflows.core.util.RequestQueue;

import java.util.stream.Stream;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aVoid;

public abstract class FunctionTracker<T> extends AbstractTracker<T> {
    private final RequestQueue requestQueue = new RequestQueue();
    private Object changeKey;

    public static <A, R> ATracker<R> track(AFunction<A, R> function, ATracker<A> argument) {
        return new FunctionTracker<R>() {
            private final TrackerAdapter<A> listener = new TrackerAdapter<>(argument, this::changed);

            @Override
            protected Stream<TrackerAdapter<?>> arguments() {
                return Stream.of(listener);
            }

            @Override
            protected Promise<R> evaluate() {
                return aNow(() -> function.apply(listener.value().value()));
            }
        }.export();
    }

    public static <A, B, R> ATracker<R> track(AFunction2<A, B, R> function, ATracker<A> argumentA, ATracker<B> argumentB) {
        return new FunctionTracker<R>() {
            private final TrackerAdapter<A> listenerA = new TrackerAdapter<>(argumentA, this::changed);
            private final TrackerAdapter<B> listenerB = new TrackerAdapter<>(argumentB, this::changed);

            @Override
            protected Stream<TrackerAdapter<?>> arguments() {
                return Stream.of(listenerA, listenerB);
            }

            @Override
            protected Promise<R> evaluate() {
                return aNow(() -> function.apply(listenerA.value().value(), listenerB.value().value()));
            }
        }.export();
    }

    public static <A, B, C, R> ATracker<R> track(AFunction3<A, B, C, R> function, ATracker<A> argumentA, ATracker<B> argumentB, ATracker<C> argumentC) {
        return new FunctionTracker<R>() {
            private final TrackerAdapter<A> listenerA = new TrackerAdapter<>(argumentA, this::changed);
            private final TrackerAdapter<B> listenerB = new TrackerAdapter<>(argumentB, this::changed);
            private final TrackerAdapter<C> listenerC = new TrackerAdapter<>(argumentC, this::changed);

            @Override
            protected Stream<TrackerAdapter<?>> arguments() {
                return Stream.of(listenerA, listenerB, listenerC);
            }

            @Override
            protected Promise<R> evaluate() {
                return aNow(() -> function.apply(listenerA.value().value(), listenerB.value().value(), listenerC.value().value()));
            }
        }.export();
    }

    public static <A, B, C, D, R> ATracker<R> track(AFunction4<A, B, C, D, R> function, ATracker<A> argumentA, ATracker<B> argumentB, ATracker<C> argumentC, ATracker<D> argumentD) {
        return new FunctionTracker<R>() {
            private final TrackerAdapter<A> listenerA = new TrackerAdapter<>(argumentA, this::changed);
            private final TrackerAdapter<B> listenerB = new TrackerAdapter<>(argumentB, this::changed);
            private final TrackerAdapter<C> listenerC = new TrackerAdapter<>(argumentC, this::changed);
            private final TrackerAdapter<D> listenerD = new TrackerAdapter<>(argumentD, this::changed);

            @Override
            protected Stream<TrackerAdapter<?>> arguments() {
                return Stream.of(listenerA, listenerB, listenerC, listenerD);
            }

            @Override
            protected Promise<R> evaluate() {
                return aNow(() -> function.apply(listenerA.value().value(), listenerB.value().value(), listenerC.value().value(), listenerD.value().value()));
            }
        }.export();
    }

    protected abstract Stream<TrackerAdapter<?>> arguments();

    protected abstract Promise<T> evaluate();

    protected void changed() {
        changeKey = new Object();
        final Object opKey = changeKey;
        requestQueue.run(() -> {
            if (opKey != changeKey) {
                return aVoid();
            }
            if (arguments().anyMatch(a -> a.value() == null)) {
                return aVoid();
            }
            final Throwable throwable = arguments().map(TrackerAdapter::value).filter(Outcome::isFailure)
                    .map(Outcome::failure).findFirst().orElse(null);
            if (throwable != null) {
                supplyValue(Outcome.failure(throwable));
                return aVoid();
            }
            return evaluate().flatMapOutcome(o -> {
                supplyValue(o);
                return aVoid();
            });
        });
    }

    @Override
    protected void subscriptionStarted() {
        arguments().forEach(TrackerAdapter::start);
    }

    @Override
    protected void subscriptionEnded() {
        arguments().forEach(TrackerAdapter::stop);
    }

}
