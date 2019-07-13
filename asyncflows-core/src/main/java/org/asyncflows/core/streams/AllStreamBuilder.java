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

package org.asyncflows.core.streams;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.RequestQueue;

import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.evaluate;
import static org.asyncflows.core.streams.AsyncStreams.aForArray;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * Stream builder for all elements.
 *
 * @param <T> a stream element
 */
public class AllStreamBuilder<T> extends StreamBuilder<T> {
    /**
     * The outcome sink under the all stream.
     */
    private final PushStreamBuilder<Outcome<T>> outcomeSink;

    /**
     * The constructor.
     *
     * @param outcomeSink the outcome sink
     */
    public AllStreamBuilder(final PushStreamBuilder<Outcome<T>> outcomeSink) {
        this.outcomeSink = outcomeSink;
    }

    /**
     * The outcome stream for the all.
     *
     * @param stream the stream
     * @param <T>    the original stream type
     * @return the streams of outcomes
     */
    public static <T> AStream<Outcome<T>> outcomeStream(final AStream<T> stream) {
        return new ChainedStreamBase<Outcome<T>, AStream<T>>(stream) {
            private final RequestQueue requests = new RequestQueue();
            private boolean eof;

            @Override
            protected Promise<Maybe<Outcome<T>>> produce() {
                return requests.run(() -> {
                    if (eof) {
                        return aMaybeEmpty();
                    }
                    return aNow(StreamUtil.producerFromStream(wrapped)).flatMapOutcome(
                            outcome -> {
                                if (outcome.isSuccess()) {
                                    if (outcome.value().isEmpty()) {
                                        return aMaybeEmpty();
                                    } else {
                                        return aMaybeValue(Outcome.<T>success(outcome.value().value()));
                                    }

                                } else {
                                    eof = true;
                                    return aMaybeValue(Outcome.<T>failure(outcome.failure()));
                                }
                            });
                });
            }
        };
    }

    @Override
    public AllStreamBuilder<T> all(final int windowSize) {
        return window(windowSize);
    }

    @Override
    public AStream<T> localStream() {
        return new ChainedStreamBase<T, AStream<Outcome<T>>>(outcomeSink.localStream()) {
            private final RequestQueue requests = new RequestQueue();

            @Override
            protected Promise<Maybe<T>> produce() {
                return requests.run(() -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    return wrapped.next().flatMap(value -> {
                        if (value.isEmpty()) {
                            return aMaybeEmpty();
                        }
                        if (value.value().isSuccess()) {
                            return aMaybeValue(value.value().value());
                        }
                        return aSeqWhile(
                                () -> wrapped.next().flatMap(discarded -> aBoolean(discarded.hasValue()))
                        ).thenFailure(value.value().failure());
                    });
                });
            }
        };
    }

    @Override
    public void connect(final ASink<? super T> consumer) {
        pull().connect(consumer);
    }

    @Override
    public AllStreamBuilder<T> push() {
        return this;
    }

    @Override
    public PullStreamBuilder<T> pull() {
        return new PullStreamBuilder<>(localStream());
    }

    @Override
    public <N> AllStreamBuilder<N> map(final AFunction<T, N> mapper) {
        return new AllStreamBuilder<>(outcomeSink.map(value -> {
            if (value.isSuccess()) {
                return evaluate(mapper, value.value()).toOutcomePromise();
            } else {
                return aValue(Outcome.<N>failure(value.failure()));
            }
        }));
    }

    @Override
    public <N> AllStreamBuilder<N> flatMapMaybe(final AFunction<T, Maybe<N>> mapper) {
        return new AllStreamBuilder<>(outcomeSink.flatMapMaybe(value -> {
            if (value.isSuccess()) {
                return evaluate(mapper, value.value()).flatMapOutcome(
                        outcome -> {
                            if (outcome.isSuccess()) {
                                if (outcome.value().isEmpty()) {
                                    return aMaybeEmpty();
                                } else {
                                    return aMaybeValue(Outcome.<N>success(outcome.value().value()));
                                }
                            } else {
                                return aMaybeValue(Outcome.<N>failure(outcome.failure()));
                            }
                        });
            } else {
                return aMaybeValue(Outcome.<N>failure(value.failure()));
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <N> AllStreamBuilder<N> flatMapStream(final AFunction<T, AStream<N>> mapper) {
        return new AllStreamBuilder<>(outcomeSink.flatMapStream(value -> {
            if (value.isSuccess()) {
                return evaluate(mapper, value.value()).flatMapOutcome(value1 -> {
                    if (value1.isSuccess()) {
                        return aValue(outcomeStream(value1.value()));
                    } else {
                        return aValue(aForArray(Outcome.<N>failure(value1.failure())).localStream());
                    }
                });
            } else {
                return aValue(aForArray(Outcome.<N>failure(value.failure())).localStream());
            }
        }));
    }

    @Override
    public AllStreamBuilder<T> window(final int size) {
        return new AllStreamBuilder<>(outcomeSink.window(size));
    }

    @Override
    public Promise<Void> consume(final AFunction<T, Boolean> loopBody) {
        return pull().consume(loopBody);
    }
}
