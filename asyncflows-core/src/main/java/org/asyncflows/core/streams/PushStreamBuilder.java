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

package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.RequestQueue;

import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.function.AsyncFunctionUtil.booleanSupplier;
import static org.asyncflows.core.function.AsyncFunctionUtil.evaluate;

/**
 * The {@link ASink}-based forward stream builder. It actually builds sinks in the reverse order. So until
 * stream is requested or iteration is started, the sink is not built.
 *
 * @param <T> the current element type
 */
public class PushStreamBuilder<T> extends StreamBuilder<T> {
    /**
     * The connector to the next sink.
     */
    private final SinkConnector<T> connector;

    /**
     * The constructor from the connector.
     *
     * @param connector the connector
     */
    public PushStreamBuilder(final SinkConnector<T> connector) {
        this.connector = connector;
    }

    @Override
    public AStream<T> localStream() {
        return pull().localStream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void connect(final ASink<? super T> consumer) {
        connector.connect((ASink<T>) consumer);
    }

    @Override
    public PushStreamBuilder<T> push() {
        return this;
    }

    @Override
    public PullStreamBuilder<T> pull() {
        final Tuple2<ASink<T>, AStream<T>> queue = RandevuQueue.local();
        connector.connect(queue.getValue1());
        return new PullStreamBuilder<>(queue.getValue2());
    }

    @Override
    public <N> PushStreamBuilder<N> map(final AFunction<T, N> mapper) {
        return new PushStreamBuilder<>(nextSink -> connector.connect(new TransformSinkBase<N, T>(nextSink) {
            @Override
            public Promise<Void> put(final T value) {
                final Promise<N> next = evaluate(mapper, value);
                return requestQueue().run(() -> next.flatMapOutcome(outcome -> {
                    if (outcome.isSuccess()) {
                        return wrapped.put(outcome.value());
                    } else {
                        return failNext(outcome.failure());
                    }
                })).listen(outcomeChecker());
            }
        }));
    }


    @Override
    public <N> PushStreamBuilder<N> flatMapMaybe(final AFunction<T, Maybe<N>> mapper) {
        return new PushStreamBuilder<>(nextSink -> connector.connect(new TransformSinkBase<N, T>(nextSink) {
            @Override
            public Promise<Void> put(final T value) {
                final Promise<Maybe<N>> next = evaluate(mapper, value);
                return requestQueue().run(() -> next.flatMapOutcome(value1 -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    if (value1.isSuccess()) {
                        return value1.value().isEmpty() ? aVoid()
                                : wrapped.put(value1.value().value());
                    } else {
                        return failNext(value1.failure());
                    }
                }).listen(outcomeChecker()));
            }
        }));
    }

    @Override
    public <N> PushStreamBuilder<N> flatMapStream(final AFunction<T, AStream<N>> mapper) {
        return new PushStreamBuilder<>(nextSink -> connector.connect(new TransformSinkBase<N, T>(nextSink) {
            @Override
            public Promise<Void> put(final T value) {
                final Promise<AStream<N>> next = evaluate(mapper, value);
                return requestQueue().run(() -> next.flatMapOutcome(value1 -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    if (value1.isSuccess()) {
                        return AsyncStreams.aForStream(value1.value()).consume(
                                value2 -> nextSink.put(value2).thenFlatGet(booleanSupplier(true)));
                    } else {
                        return failNext(value1.failure());
                    }
                }).listen(outcomeChecker()));
            }
        }));
    }


    @Override
    public PushStreamBuilder<T> window(final int size) {
        return new PushStreamBuilder<>(new SinkConnector<T>() {
            @Override
            public void connect(final ASink<T> nextSink) {
                connector.connect(new TransformSinkBase<T, T>(nextSink) {
                    private int active;
                    private final AResolver<Void> countdownObserver = outcome -> {
                        active--;
                        requestQueue().resume();
                    };

                    @Override
                    protected void onInvalidation(final Throwable throwable) {
                        active = Integer.MIN_VALUE;
                        requestQueue().resume();
                        super.onInvalidation(throwable);
                    }

                    @Override
                    public Promise<Void> put(final T value) {
                        return requestQueue().runSeqWhile(() -> {
                            if (!isValidAndOpen()) {
                                return invalidationPromise();
                            }
                            if (active < size) {
                                aNow(() -> nextSink.put(value)).listen(outcomeChecker()).listen(countdownObserver);
                                return aFalse();
                            }
                            return requestQueue().suspendThenTrue();
                        });
                    }
                });
            }
        });
    }

    @Override
    public Promise<Void> consume(final AFunction<T, Boolean> loopBody) {
        final SinkBase<T> lastSink = new SinkBase<T>() {
            private final RequestQueue requests = new RequestQueue();

            @Override
            public Promise<Void> put(final T value) {
                return requests.run(() -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    return evaluate(loopBody, value).flatMap(value1 -> {
                        if (!value1) {
                            startClosing();
                        }
                        return aVoid();
                    }).listen(outcomeChecker());
                });
            }
        };
        connector.connect(lastSink);
        return lastSink.finished();
    }

    /**
     * The interface that allows connecting to the next sink.
     *
     * @param <T> the next sink type
     */
    interface SinkConnector<T> {
        /**
         * Connect to the next sink.
         *
         * @param nextSink the next sink
         */
        void connect(ASink<T> nextSink);
    }
}
