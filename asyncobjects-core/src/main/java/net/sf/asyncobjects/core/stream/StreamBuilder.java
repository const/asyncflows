package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.ProducerUtil;
import net.sf.asyncobjects.core.util.RequestQueue;
import net.sf.asyncobjects.core.util.ResourceUtil;

import java.util.ArrayDeque;

import static net.sf.asyncobjects.core.AsyncControl.aBoolean;
import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.util.ProducerUtil.aEmptyOption;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqOptionLoop;

/**
 * The stream builder provides fluent interfaces for building streams.
 *
 * @param <T> the stream element type
 */
public class StreamBuilder<T> extends ForwardStreamBuilder<T> {
    /**
     * The current stream.
     */
    private final AStream<T> current;

    /**
     * The constructor for the builder.
     *
     * @param current the current stream.
     */
    public StreamBuilder(final AStream<T> current) {
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
    public SinkStreamBuilder<T> push() {
        return new SinkStreamBuilder<T>(new SinkStreamBuilder.SinkConnector<T>() {
            @Override
            public void connect(final ASink<T> nextSink) {
                StreamUtil.connect(current, nextSink);
            }
        });
    }

    @Override
    public ForwardStreamBuilder<T> pull() {
        return this;
    }

    @Override
    public <N> StreamBuilder<N> map(final AFunction<N, T> mapper) {
        final AFunction<Maybe<N>, Maybe<T>> producerMapper = ProducerUtil.toProducerMapper(mapper);
        return new StreamBuilder<N>(new ChainedStreamBase<N, AStream<T>>(current) {
            @Override
            protected Promise<Maybe<N>> produce() {
                return wrapped.next().map(producerMapper);
            }
        });
    }

    @Override
    public <N> ForwardStreamBuilder<N> flatMapStream(final AFunction<AStream<N>, T> mapper) {
        return new StreamBuilder<N>(new ChainedStreamBase<N, AStream<AStream<N>>>(map(mapper).current) {
            private final RequestQueue requests = new RequestQueue();
            private AStream<N> mapped;
            private boolean eof;

            @Override
            protected Promise<Maybe<N>> produce() {
                return requests.run(new ACallable<Maybe<N>>() {
                    @Override
                    public Promise<Maybe<N>> call() throws Throwable {
                        return aSeqOptionLoop(new ACallable<Maybe<Maybe<N>>>() {
                            @Override
                            public Promise<Maybe<Maybe<N>>> call() throws Throwable {
                                if (mapped == null) {
                                    if (eof) {
                                        return aSuccess(Maybe.<Maybe<N>>value(Maybe.<N>empty()));
                                    }
                                    return wrapped.next().map(new AFunction<Maybe<Maybe<N>>, Maybe<AStream<N>>>() {
                                        @Override
                                        public Promise<Maybe<Maybe<N>>> apply(final Maybe<AStream<N>> value) {
                                            if (value.isEmpty()) {
                                                eof = true;
                                                return aSuccess(Maybe.<Maybe<N>>value(Maybe.<N>empty()));
                                            }
                                            mapped = value.value();
                                            return aEmptyOption();
                                        }
                                    });
                                }
                                return mapped.next().map(new AFunction<Maybe<Maybe<N>>, Maybe<N>>() {
                                    @Override
                                    public Promise<Maybe<Maybe<N>>> apply(final Maybe<N> value) throws Throwable {
                                        if (value.isEmpty()) {
                                            mapped = null;
                                            return aEmptyOption();
                                        } else {
                                            return aSuccess(Maybe.<Maybe<N>>value(value));
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public ForwardStreamBuilder<T> window(final int size) {
        final RequestQueue writes = new RequestQueue();
        final RequestQueue reads = new RequestQueue();
        final ArrayDeque<Outcome<Maybe<T>>> elements = new ArrayDeque<Outcome<Maybe<T>>>(size);
        final boolean[] closed = new boolean[1];
        // prefetch process, it never fails
        writes.runSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                if (closed[0]) {
                    elements.clear();
                    return aFalse();
                }
                if (elements.size() >= size) {
                    return writes.suspendThenTrue();
                }
                return aNow(new ACallable<Maybe<T>>() {
                    @Override
                    public Promise<Maybe<T>> call() throws Throwable {
                        return stream().next();
                    }
                }).mapOutcome(new AFunction<Boolean, Outcome<Maybe<T>>>() {
                    @Override
                    public Promise<Boolean> apply(final Outcome<Maybe<T>> value) throws Throwable {
                        elements.addLast(value);
                        reads.resume();
                        return aBoolean(value.isSuccess() && value.value().hasValue());
                    }
                });
            }
        });
        return new StreamBuilder<T>(new StreamBase<T>() {
            @Override
            public Promise<Maybe<T>> produce() throws Throwable {
                return reads.runSeqOptionLoop(new ACallable<Maybe<Maybe<T>>>() {
                    @Override
                    public Promise<Maybe<Maybe<T>>> call() throws Throwable {
                        if (!isValidAndOpen()) {
                            return invalidationPromise();
                        }
                        if (elements.isEmpty()) {
                            return reads.suspendThenEmpty();
                        }
                        final Outcome<Maybe<T>> element = elements.removeFirst();
                        writes.resume();
                        if (element.isSuccess()) {
                            return aSuccess(Maybe.value(element.value()));
                        } else {
                            return aFailure(element.failure());
                        }
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
    public Promise<Void> consume(final AFunction<Boolean, T> loopBody) {
        final AStream<T> stream = current;
        return aSeq(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aSeqLoop(new ACallable<Boolean>() {
                    @Override
                    public Promise<Boolean> call() throws Throwable {
                        return stream.next().map(new AFunction<Boolean, Maybe<T>>() {
                            @Override
                            public Promise<Boolean> apply(final Maybe<T> value) throws Throwable {
                                if (value.isEmpty()) {
                                    return aFalse();
                                }
                                return loopBody.apply(value.value());
                            }
                        });
                    }
                });
            }
        }).finallyDo(ResourceUtil.closeResourceAction(current));
    }

    @Override
    public <N> StreamBuilder<N> flatMapMaybe(final AFunction<Maybe<N>, T> mapper) {
        return new StreamBuilder<N>(new ChainedStreamBase<N, AStream<Maybe<N>>>(map(mapper).current) {
            private final RequestQueue requests = new RequestQueue();

            @Override
            protected Promise<Maybe<N>> produce() {
                return requests.run(new ACallable<Maybe<N>>() {
                    @Override
                    public Promise<Maybe<N>> call() throws Throwable {
                        return aSeqOptionLoop(new ACallable<Maybe<Maybe<N>>>() {
                            @Override
                            public Promise<Maybe<Maybe<N>>> call() throws Throwable {
                                return wrapped.next().map(new AFunction<Maybe<Maybe<N>>, Maybe<Maybe<N>>>() {
                                    @Override
                                    public Promise<Maybe<Maybe<N>>> apply(final Maybe<Maybe<N>> value) {
                                        if (value.isEmpty()) {
                                            return aSuccess(Maybe.<Maybe<N>>value(Maybe.<N>empty()));
                                        } else if (value.value().isEmpty()) {
                                            return aEmptyOption();
                                        } else {
                                            return aSuccess(value);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }
}
