package org.asyncflows.core.streams;


import org.asyncflows.core.AsyncControl;
import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.util.ProducerUtil;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.util.ResourceUtil;

import java.util.ArrayDeque;

import static org.asyncflows.core.AsyncControl.aBoolean;
import static org.asyncflows.core.AsyncControl.aFailure;
import static org.asyncflows.core.AsyncControl.aFalse;
import static org.asyncflows.core.AsyncControl.aMaybeEmpty;
import static org.asyncflows.core.AsyncControl.aMaybeValue;
import static org.asyncflows.core.AsyncControl.aNow;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.util.AsyncSeqControl.aSeq;
import static org.asyncflows.core.util.AsyncSeqControl.aSeqUntilValue;
import static org.asyncflows.core.util.AsyncSeqControl.aSeqWhile;

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
                            return aMaybeValue(Maybe.<N>empty());
                        }
                        return wrapped.next().flatMap((Maybe<AStream<N>> value) -> {
                            if (value.isEmpty()) {
                                eof = true;
                                return aMaybeValue(Maybe.<N>empty());
                            }
                            mapped = value.value();
                            return AsyncControl.<Maybe<N>>aMaybeEmpty();
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
            protected Promise<Maybe<T>> produce() throws Throwable {
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

    @Override
    public Promise<Void> consume(final AFunction<T, Boolean> loopBody) {
        final AStream<T> stream = current;
        return aSeq(() -> aSeqWhile(() -> stream.next().flatMap(value -> {
            if (value.isEmpty()) {
                return aFalse();
            }
            return loopBody.apply(value.value());
        }))).finallyDo(ResourceUtil.closeResourceAction(current));
    }

    @Override
    public <N> PullStreamBuilder<N> flatMapMaybe(final AFunction<T, Maybe<N>> mapper) {
        return new PullStreamBuilder<>(new ChainedStreamBase<N, AStream<Maybe<N>>>(map(mapper).current) {
            private final RequestQueue requests = new RequestQueue();

            @Override
            protected Promise<Maybe<N>> produce() {
                return requests.runSeqUntilValue(() -> wrapped.next().flatMap(value -> {
                    if (value.isEmpty()) {
                        return aMaybeValue(Maybe.<N>empty());
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
