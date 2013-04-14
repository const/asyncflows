package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.RequestQueue;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.CoreFunctionUtil.evaluate;
import static net.sf.asyncobjects.core.util.ProducerUtil.aEmptyOption;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqOptionLoop;

/**
 * Stream builder for all elements.
 *
 * @param <T> a stream element
 */
public class AllStreamBuilder<T> extends ForwardStreamBuilder<T> {
    /**
     * The outcome sink under the all stream.
     */
    private final SinkStreamBuilder<Outcome<T>> outcomeSink;

    /**
     * The constructor.
     *
     * @param outcomeSink the outcome sink
     */
    public AllStreamBuilder(final SinkStreamBuilder<Outcome<T>> outcomeSink) {
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
            private boolean eof;
            private final RequestQueue requests = new RequestQueue();

            @Override
            protected Promise<Maybe<Outcome<T>>> produce() {
                return requests.run(new ACallable<Maybe<Outcome<T>>>() {
                    @Override
                    public Promise<Maybe<Outcome<T>>> call() throws Throwable {
                        if (eof) {
                            return aEmptyOption();
                        }
                        return aNow(new ACallable<Maybe<T>>() {
                            @Override
                            public Promise<Maybe<T>> call() throws Throwable {
                                return wrapped.next();
                            }
                        }).mapOutcome(new AFunction<Maybe<Outcome<T>>, Outcome<Maybe<T>>>() {
                            @Override
                            public Promise<Maybe<Outcome<T>>> apply(final Outcome<Maybe<T>> value) {
                                if (value.isSuccess()) {
                                    if (value.value().isEmpty()) {
                                        return aEmptyOption();
                                    } else {
                                        return aSuccess(Maybe.value(Outcome.<T>success(value.value().value())));
                                    }

                                } else {
                                    eof = true;
                                    return aSuccess(Maybe.value(Outcome.<T>failure(value.failure())));
                                }
                            }
                        });
                    }
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
                return requests.run(new ACallable<Maybe<T>>() {
                    @Override
                    public Promise<Maybe<T>> call() throws Throwable {
                        if (!isValidAndOpen()) {
                            return invalidationPromise();
                        }
                        return wrapped.next().map(new AFunction<Maybe<T>, Maybe<Outcome<T>>>() {
                            @Override
                            public Promise<Maybe<T>> apply(final Maybe<Outcome<T>> value) throws Throwable {
                                if (value.isEmpty()) {
                                    return aEmptyOption();
                                }
                                if (value.value().isSuccess()) {
                                    return aSuccess(Maybe.value(value.value().value()));
                                }
                                return aSeqOptionLoop(new ACallable<Maybe<Maybe<T>>>() {
                                    @Override
                                    public Promise<Maybe<Maybe<T>>> call() throws Throwable {
                                        return wrapped.next().map(new AFunction<Maybe<Maybe<T>>, Maybe<Outcome<T>>>() {
                                            @Override
                                            public Promise<Maybe<Maybe<T>>> apply(final Maybe<Outcome<T>> discarded) {
                                                if (discarded.isEmpty()) {
                                                    return aFailure(value.value().failure());
                                                } else {
                                                    // continue loop
                                                    return aEmptyOption();
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
    public StreamBuilder<T> pull() {
        return new StreamBuilder<T>(localStream());
    }

    @Override
    public <N> AllStreamBuilder<N> map(final AFunction<N, T> mapper) {
        return new AllStreamBuilder<N>(outcomeSink.map(new AFunction<Outcome<N>, Outcome<T>>() {
            @Override
            public Promise<Outcome<N>> apply(final Outcome<T> value) throws Throwable {
                if (value.isSuccess()) {
                    return evaluate(value.value(), mapper).toOutcomePromise();
                } else {
                    return aSuccess(Outcome.<N>failure(value.failure()));
                }
            }
        }));
    }

    @Override
    public <N> AllStreamBuilder<N> flatMapMaybe(final AFunction<Maybe<N>, T> mapper) {
        return new AllStreamBuilder<N>(outcomeSink.flatMapMaybe(new AFunction<Maybe<Outcome<N>>, Outcome<T>>() {
            @Override
            public Promise<Maybe<Outcome<N>>> apply(final Outcome<T> value) throws Throwable {
                if (value.isSuccess()) {
                    return evaluate(value.value(), mapper).mapOutcome(
                            new AFunction<Maybe<Outcome<N>>, Outcome<Maybe<N>>>() {
                                @Override
                                public Promise<Maybe<Outcome<N>>> apply(final Outcome<Maybe<N>> value) {
                                    if (value.isSuccess()) {
                                        if (value.value().isEmpty()) {
                                            return aEmptyOption();
                                        } else {
                                            return aSuccess(Maybe.<Outcome<N>>value(
                                                    Outcome.<N>success(value.value().value())));
                                        }
                                    } else {
                                        return aSuccess(Maybe.<Outcome<N>>value(Outcome.<N>failure(value.failure())));
                                    }
                                }
                            });
                } else {
                    return aSuccess(Maybe.value(Outcome.<N>failure(value.failure())));
                }
            }
        }));
    }

    @Override
    public <N> AllStreamBuilder<N> flatMapStream(final AFunction<AStream<N>, T> mapper) {
        return new AllStreamBuilder<N>(outcomeSink.flatMapStream(new AFunction<AStream<Outcome<N>>, Outcome<T>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Promise<AStream<Outcome<N>>> apply(final Outcome<T> value) throws Throwable {
                if (value.isSuccess()) {
                    return evaluate(value.value(), mapper).mapOutcome(new AFunction<AStream<Outcome<N>>,
                            Outcome<AStream<N>>>() {
                        @Override
                        public Promise<AStream<Outcome<N>>> apply(final Outcome<AStream<N>> value) {
                            if (value.isSuccess()) {
                                return aSuccess(outcomeStream(value.value()));
                            } else {
                                return aSuccess(Streams.aForArray(Outcome.<N>failure(value.failure())).localStream());
                            }
                        }
                    });
                } else {
                    return aSuccess(Streams.aForArray(Outcome.<N>failure(value.failure())).localStream());
                }
            }
        }));
    }

    @Override
    public AllStreamBuilder<T> window(final int size) {
        return new AllStreamBuilder<T>(outcomeSink.window(size));
    }

    @Override
    public Promise<Void> consume(final AFunction<Boolean, T> loopBody) {
        return pull().consume(loopBody);
    }
}
