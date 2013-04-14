package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.util.RequestQueue;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.CoreFunctionUtil.booleanCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.evaluate;

/**
 * The {@link ASink}-based forward stream builder. It actually builds sinks in the reverse order. So until
 * stream is requested or iteration is started, the sink is not built.
 *
 * @param <T> the current element type
 */
public class SinkStreamBuilder<T> extends ForwardStreamBuilder<T> {
    /**
     * The connector to the next sink.
     */
    private final SinkConnector<T> connector;

    /**
     * The constructor from the connector.
     *
     * @param connector the connector
     */
    public SinkStreamBuilder(final SinkConnector<T> connector) {
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
    public SinkStreamBuilder<T> push() {
        return this;
    }

    @Override
    public StreamBuilder<T> pull() {
        final Tuple2<ASink<T>, AStream<T>> queue = RandevuQueue.local();
        connector.connect(queue.getValue1());
        return new StreamBuilder<T>(queue.getValue2());
    }

    @Override
    public <N> SinkStreamBuilder<N> map(final AFunction<N, T> mapper) {
        return new SinkStreamBuilder<N>(new SinkConnector<N>() {
            @Override
            public void connect(final ASink<N> nextSink) {
                connector.connect(new TransformSinkBase<N, T>(nextSink) {
                    @Override
                    public Promise<Void> put(final T value) {
                        final Promise<N> next = evaluate(value, mapper);
                        return requestQueue().run(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return next.mapOutcome(new AFunction<Void, Outcome<N>>() {
                                    @Override
                                    public Promise<Void> apply(final Outcome<N> value) throws Throwable {
                                        if (value.isSuccess()) {
                                            return wrapped.put(value.value());
                                        } else {
                                            return failNext(value.failure());
                                        }
                                    }
                                });
                            }
                        }).observe(outcomeChecker());
                    }
                });

            }
        });
    }

    @Override
    public <N> SinkStreamBuilder<N> flatMapMaybe(final AFunction<Maybe<N>, T> mapper) {
        return new SinkStreamBuilder<N>(new SinkConnector<N>() {
            @Override
            public void connect(final ASink<N> nextSink) {
                connector.connect(new TransformSinkBase<N, T>(nextSink) {
                    @Override
                    public Promise<Void> put(final T value) {
                        final Promise<Maybe<N>> next = evaluate(value, mapper);
                        return requestQueue().run(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return next.mapOutcome(new AFunction<Void, Outcome<Maybe<N>>>() {
                                    @Override
                                    public Promise<Void> apply(final Outcome<Maybe<N>> value) throws Throwable {
                                        if (!isValidAndOpen()) {
                                            return invalidationPromise();
                                        }
                                        if (value.isSuccess()) {
                                            return value.value().isEmpty() ? aVoid()
                                                    : wrapped.put(value.value().value());
                                        } else {
                                            return failNext(value.failure());
                                        }
                                    }
                                }).observe(outcomeChecker());
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public <N> SinkStreamBuilder<N> flatMapStream(final AFunction<AStream<N>, T> mapper) {
        return new SinkStreamBuilder<N>(new SinkConnector<N>() {
            @Override
            public void connect(final ASink<N> nextSink) {
                connector.connect(new TransformSinkBase<N, T>(nextSink) {
                    @Override
                    public Promise<Void> put(final T value) {
                        final Promise<AStream<N>> next = evaluate(value, mapper);
                        return requestQueue().run(new ACallable<Void>() {
                            @Override
                            public Promise<Void> call() throws Throwable {
                                return next.mapOutcome(new AFunction<Void, Outcome<AStream<N>>>() {
                                    @Override
                                    public Promise<Void> apply(final Outcome<AStream<N>> value) throws Throwable {
                                        if (!isValidAndOpen()) {
                                            return invalidationPromise();
                                        }
                                        if (value.isSuccess()) {
                                            return Streams.aForStream(value.value()).consume(
                                                    new AFunction<Boolean, N>() {
                                                        @Override
                                                        public Promise<Boolean> apply(final N value) {
                                                            return nextSink.put(value).then(booleanCallable(true));
                                                        }
                                                    });
                                        } else {
                                            return failNext(value.failure());
                                        }
                                    }
                                }).observe(outcomeChecker());
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public SinkStreamBuilder<T> window(final int size) {
        return new SinkStreamBuilder<T>(new SinkConnector<T>() {
            @Override
            public void connect(final ASink<T> nextSink) {
                connector.connect(new TransformSinkBase<T, T>(nextSink) {
                    private int active;
                    private AResolver<Void> countdownObserver = new AResolver<Void>() {
                        @Override
                        public void resolve(final Outcome<Void> resolution) throws Throwable {
                            active--;
                            requestQueue().resume();
                        }
                    };

                    @Override
                    protected void onInvalidation(final Throwable throwable) {
                        active = Integer.MIN_VALUE;
                        requestQueue().resume();
                        super.onInvalidation(throwable);
                    }

                    @Override
                    public Promise<Void> put(final T value) {
                        return requestQueue().runSeqLoop(new ACallable<Boolean>() {
                            @Override
                            public Promise<Boolean> call() throws Throwable {
                                if (!isValidAndOpen()) {
                                    return invalidationPromise();
                                }
                                if (active < size) {
                                    aNow(new ACallable<Void>() {
                                        @Override
                                        public Promise<Void> call() throws Throwable {
                                            return nextSink.put(value);
                                        }
                                    }).observe(outcomeChecker()).listen(countdownObserver);
                                    return aFalse();
                                }
                                return requestQueue().suspendThenTrue();
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public Promise<Void> consume(final AFunction<Boolean, T> loopBody) {
        final SinkBase<T> lastSink = new SinkBase<T>() {
            private final RequestQueue requests = new RequestQueue();

            @Override
            public Promise<Void> put(final T value) {
                return requests.run(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        if (!isValidAndOpen()) {
                            return invalidationPromise();
                        }
                        return evaluate(value, loopBody).map(new AFunction<Void, Boolean>() {
                            @Override
                            public Promise<Void> apply(final Boolean value) throws Throwable {
                                if (!value) {
                                    startClosing();
                                }
                                return aVoid();
                            }
                        }).observe(outcomeChecker());
                    }
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
