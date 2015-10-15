package net.sf.asyncobjects.core.stream;

import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.util.RequestQueue;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeEmpty;
import static net.sf.asyncobjects.core.AsyncControl.aMaybeValue;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.CoreFunctionUtil.evaluate;
import static net.sf.asyncobjects.core.stream.Streams.aForArray;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqMaybeLoopFair;

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
            private final RequestQueue requests = new RequestQueue();
            private boolean eof;

            @Override
            protected Promise<Maybe<Outcome<T>>> produce() {
                return requests.run(() -> {
                    if (eof) {
                        return aMaybeEmpty();
                    }
                    return aNow(StreamUtil.producerFromStream(wrapped)).mapOutcome(
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
                    return wrapped.next().map(value -> {
                        if (value.isEmpty()) {
                            return aMaybeEmpty();
                        }
                        if (value.value().isSuccess()) {
                            return aMaybeValue(value.value().value());
                        }
                        return aSeqMaybeLoopFair(() -> wrapped.next().map(discarded -> {
                            if (discarded.isEmpty()) {
                                return aFailure(value.value().failure());
                            } else {
                                // continue loop
                                return aMaybeEmpty();
                            }
                        }));
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
    public StreamBuilder<T> pull() {
        return new StreamBuilder<>(localStream());
    }

    @Override
    public <N> AllStreamBuilder<N> map(final AFunction<N, T> mapper) {
        return new AllStreamBuilder<>(outcomeSink.map(value -> {
            if (value.isSuccess()) {
                return evaluate(value.value(), mapper).toOutcomePromise();
            } else {
                return aValue(Outcome.<N>failure(value.failure()));
            }
        }));
    }

    @Override
    public <N> AllStreamBuilder<N> flatMapMaybe(final AFunction<Maybe<N>, T> mapper) {
        return new AllStreamBuilder<>(outcomeSink.flatMapMaybe(value -> {
            if (value.isSuccess()) {
                return evaluate(value.value(), mapper).mapOutcome(
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
    public <N> AllStreamBuilder<N> flatMapStream(final AFunction<AStream<N>, T> mapper) {
        return new AllStreamBuilder<>(outcomeSink.flatMapStream(value -> {
            if (value.isSuccess()) {
                return evaluate(value.value(), mapper).mapOutcome(value1 -> {
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
        return new AllStreamBuilder<T>(outcomeSink.window(size));
    }

    @Override
    public Promise<Void> consume(final AFunction<Boolean, T> loopBody) {
        return pull().consume(loopBody);
    }
}
