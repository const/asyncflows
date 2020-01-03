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

package org.asyncflows.core.streams;


import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.CoreFlowsResource;
import org.asyncflows.core.util.ProducerUtil;
import org.asyncflows.core.util.RequestQueue;

import java.util.ArrayDeque;

import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aFalse;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;

/**
 * The stream builder provides fluent interfaces for building streams.
 *
 * @param <T> the stream element type
 */
public class PullStreamBuilder<T> extends StreamBuilder<T> {
    /**
     * The current stream.
     */
    private final AStream<T> current;

    /**
     * The constructor for the builder.
     *
     * @param current the current stream.
     */
    public PullStreamBuilder(final AStream<T> current) {
        this.current = current;
    }

    @Override
    public AStream<T> localStream() {
        return current;
    }

    @Override
    public void connect(final ASink<? super T> consumer) {
        StreamUtil.connect(current, consumer);
    }

    @Override
    public PushStreamBuilder<T> push() {
        return new PushStreamBuilder<>(nextSink -> StreamUtil.connect(current, nextSink));
    }

    @Override
    public StreamBuilder<T> pull() {
        return this;
    }

    @Override
    public <N> PullStreamBuilder<N> map(final AFunction<T, N> mapper) {
        final AFunction<Maybe<T>, Maybe<N>> producerMapper = ProducerUtil.toProducerMapper(mapper);
        return new PullStreamBuilder<>(new ChainedStreamBase<N, AStream<T>>(current) {
            @Override
            protected Promise<Maybe<N>> produce() {
                return wrapped.next().flatMap(producerMapper);
            }
        });
    }

    @SuppressWarnings("squid:S3776")
    @Override
    public <N> StreamBuilder<N> flatMapStream(final AFunction<T, AStream<N>> mapper) {
        return new PullStreamBuilder<>(new ChainedStreamBase<N, AStream<AStream<N>>>(map(mapper).current) {
            private final RequestQueue requests = new RequestQueue();
            private AStream<N> mapped;
            private boolean eof;

            @Override
            protected Promise<Maybe<N>> produce() {
                return requests.run(() -> aSeqUntilValue(() -> {
                    if (mapped == null) {
                        if (eof) {
                            return aMaybeValue(Maybe.empty());
                        }
                        return wrapped.next().flatMap((Maybe<AStream<N>> value) -> {
                            if (value.isEmpty()) {
                                eof = true;
                                return aMaybeValue(Maybe.empty());
                            }
                            mapped = value.value();
                            return CoreFlows.aMaybeEmpty();
                        });
                    }
                    return mapped.next().flatMap(value -> {
                        if (value.isEmpty()) {
                            mapped = null;
                            return aMaybeEmpty();
                        } else {
                            return aMaybeValue(value);
                        }
                    });
                }));
            }
        });
    }

    @Override
    public StreamBuilder<T> window(final int size) {
        final RequestQueue writes = new RequestQueue();
        final RequestQueue reads = new RequestQueue();
        final ArrayDeque<Outcome<Maybe<T>>> elements = new ArrayDeque<>(size);
        final boolean[] closed = new boolean[1];
        // prefetch process, it never fails
        writes.runSeqWhile(() -> {
            if (closed[0]) {
                elements.clear();
                return aFalse();
            }
            if (elements.size() >= size) {
                return writes.suspendThenTrue();
            }
            return aNow(() -> stream().next()).flatMapOutcome(value -> {
                elements.addLast(value);
                reads.resume();
                return aBoolean(value.isSuccess() && value.value().hasValue());
            });
        });
        return new PullStreamBuilder<>(new StreamBase<T>() {
            @Override
            protected Promise<Maybe<T>> produce() {
                return reads.runSeqUntilValue(() -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    if (elements.isEmpty()) {
                        return reads.suspendThenEmpty();
                    }
                    final Outcome<Maybe<T>> element = elements.removeFirst();
                    writes.resume();
                    if (element.isSuccess()) {
                        return aMaybeValue(element.value());
                    } else {
                        return aFailure(element.failure());
                    }
                });
            }

            @Override
            protected Promise<Void> closeAction() {
                closed[0] = true;
                return super.closeAction();
            }
        });
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public Promise<Void> consume(final AFunction<T, Boolean> loopBody) {
        final AStream<T> stream = current;
        return aSeq(() -> aSeqWhile(() -> stream.next().flatMap(value -> {
            if (value.isEmpty()) {
                return aFalse();
            }
            return loopBody.apply(value.value());
        }))).finallyDo(CoreFlowsResource.closeResourceAction(current));
    }

    @Override
    public <N> PullStreamBuilder<N> flatMapMaybe(final AFunction<T, Maybe<N>> mapper) {
        return new PullStreamBuilder<>(new ChainedStreamBase<N, AStream<Maybe<N>>>(map(mapper).current) {
            private final RequestQueue requests = new RequestQueue();

            @Override
            protected Promise<Maybe<N>> produce() {
                return requests.runSeqUntilValue(() -> wrapped.next().flatMap(value -> {
                    if (value.isEmpty()) {
                        return aMaybeValue(Maybe.empty());
                    } else if (value.value().isEmpty()) {
                        return aMaybeEmpty();
                    } else {
                        return aValue(value);
                    }
                }));
            }
        });
    }
}
