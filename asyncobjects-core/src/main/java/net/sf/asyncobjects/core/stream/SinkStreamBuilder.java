package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
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
        return new StreamBuilder<>(queue.getValue2());
    }

    @Override
    public <N> SinkStreamBuilder<N> map(final AFunction<N, T> mapper) {
        return new SinkStreamBuilder<>(nextSink -> connector.connect(new TransformSinkBase<N, T>(nextSink) {
            @Override
            public Promise<Void> put(final T value) {
                final Promise<N> next = evaluate(value, mapper);
                return requestQueue().run(() -> next.mapOutcome(outcome -> {
                    if (outcome.isSuccess()) {
                        return wrapped.put(outcome.value());
                    } else {
                        return failNext(outcome.failure());
                    }
                })).observe(outcomeChecker());
            }
        }));
    }

    @Override
    public <N> SinkStreamBuilder<N> flatMapMaybe(final AFunction<Maybe<N>, T> mapper) {
        return new SinkStreamBuilder<>(nextSink -> connector.connect(new TransformSinkBase<N, T>(nextSink) {
            @Override
            public Promise<Void> put(final T value) {
                final Promise<Maybe<N>> next = evaluate(value, mapper);
                return requestQueue().run(() -> next.mapOutcome(value1 -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    if (value1.isSuccess()) {
                        return value1.value().isEmpty() ? aVoid()
                                : wrapped.put(value1.value().value());
                    } else {
                        return failNext(value1.failure());
                    }
                }).observe(outcomeChecker()));
            }
        }));
    }

    @Override
    public <N> SinkStreamBuilder<N> flatMapStream(final AFunction<AStream<N>, T> mapper) {
        return new SinkStreamBuilder<>(nextSink -> connector.connect(new TransformSinkBase<N, T>(nextSink) {
            @Override
            public Promise<Void> put(final T value) {
                final Promise<AStream<N>> next = evaluate(value, mapper);
                return requestQueue().run(() -> next.mapOutcome(value1 -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    if (value1.isSuccess()) {
                        return Streams.aForStream(value1.value()).consume(
                                value2 -> nextSink.put(value2).thenDo(booleanCallable(true)));
                    } else {
                        return failNext(value1.failure());
                    }
                }).observe(outcomeChecker()));
            }
        }));
    }

    @Override
    public SinkStreamBuilder<T> window(final int size) {
        return new SinkStreamBuilder<>(new SinkConnector<T>() {
            @Override
            public void connect(final ASink<T> nextSink) {
                connector.connect(new TransformSinkBase<T, T>(nextSink) {
                    private int active;
                    private AResolver<Void> countdownObserver = (outcome) -> {
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
                        return requestQueue().runSeqLoop(() -> {
                            if (!isValidAndOpen()) {
                                return invalidationPromise();
                            }
                            if (active < size) {
                                aNow(() -> nextSink.put(value)).observe(outcomeChecker()).listen(countdownObserver);
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
    public Promise<Void> consume(final AFunction<Boolean, T> loopBody) {
        final SinkBase<T> lastSink = new SinkBase<T>() {
            private final RequestQueue requests = new RequestQueue();

            @Override
            public Promise<Void> put(final T value) {
                return requests.run(() -> {
                    if (!isValidAndOpen()) {
                        return invalidationPromise();
                    }
                    return evaluate(value, loopBody).map(value1 -> {
                        if (!value1) {
                            startClosing();
                        }
                        return aVoid();
                    }).observe(outcomeChecker());
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
