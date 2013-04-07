package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Failure;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.ResolverUtil;
import net.sf.asyncobjects.core.vats.Vat;

import java.util.ArrayList;

import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aSuccess;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.ResolverUtil.notifyResolver;

/**
 * An utility class for sequential control constructs.
 */
public final class SeqControl {
    /**
     * Private constructor for utility class.
     */
    private SeqControl() {
    }

    /**
     * The sequence operator.
     *
     * @param start the start
     * @param <T>   the initial type
     * @return the builder for the sequence
     */
    public static <T> SeqBuilder<T> aSeq(final ACallable<T> start) {
        return new SeqBuilder<T>(start);
    }

    /**
     * Iterate over collection.
     *
     * @param iterable the collection
     * @param <T>      the value to iterate over
     * @return the builder for seq for
     */
    public static <T> SeqForBuilder<T> aSeqForCollection(final Iterable<T> iterable) {
        final ACallable<OptionalValue<T>> producer = ProducerUtil.fromIterable(iterable);
        return new SeqForBuilder<T>(producer);
    }

    /**
     * Iterate over range.
     *
     * @param start the start of range
     * @param end   the end of range
     * @return the builder for seq for
     */
    public static SeqForBuilder<Integer> aSeqForRange(final int start, final int end) {
        final ACallable<OptionalValue<Integer>> producer = ProducerUtil.fromRange(start, end);
        return new SeqForBuilder<Integer>(producer);
    }


    /**
     * Generic loop that executes body until it returns false.
     *
     * @param body the body to execute
     * @return the promises that resolves when body fails or returns true
     */
    public static Promise<Void> aSeqLoop(final ACallable<Boolean> body) {
        final Promise<Void> result = new Promise<Void>();
        final AResolver<Void> resolver = result.resolver();
        final Vat vat = Vat.current();
        ResolverUtil.notifySuccess(new AResolver<Boolean>() {
            private final AResolver<Boolean> self = this;

            @Override
            public void resolve(final Outcome<Boolean> resolution) throws Throwable {
                if (resolution.isSuccess()) {
                    if (resolution.value()) {
                        vat.execute(vat, new Runnable() {
                            @Override
                            public void run() {
                                aNow(body).listen(self);
                            }
                        });
                    } else {
                        ResolverUtil.notifySuccess(resolver, null);
                    }
                } else {
                    ResolverUtil.notifyFailure(resolver, resolution.failure());
                }
            }
        }, true);
        return result;
    }

    /**
     * Builder for the sequence of operations.
     *
     * @param <T> the current result type
     */
    public static final class SeqBuilder<T> {
        /**
         * The action to execute.
         */
        private final ACallable<T> action;

        /**
         * The constructor.
         *
         * @param action the action to start with
         */
        public SeqBuilder(final ACallable<T> action) {
            this.action = action;
        }

        /**
         * @return finish the sequence
         */
        public Promise<T> finish() {
            return aNow(action);
        }


        /**
         * Add next step to the sequence.
         *
         * @param mapper the mapper
         * @param <N>    the next type
         * @return the sequence builder with next step
         */
        public <N> SeqBuilder<N> then(final AFunction<N, T> mapper) {
            return new SeqBuilder<N>(new ACallable<N>() {
                @Override
                public Promise<N> call() throws Throwable {
                    return aNow(action).map(mapper);
                }
            });
        }

        /**
         * Add next step to the sequence.
         *
         * @param mapper the mapper
         * @param <N>    the next type
         * @return the sequence builder with next step
         */
        public <N> Promise<N> thenLast(final AFunction<N, T> mapper) {
            return then(mapper).finish();
        }


        /**
         * Add next step to the sequence.
         *
         * @param nextAction the action that ignores the input argument
         * @param <N>        the next type
         * @return the sequence builder with next step
         */
        public <N> SeqBuilder<N> thenI(final ACallable<N> nextAction) {
            return then(new AFunction<N, T>() {
                @Override
                public Promise<N> apply(final T value) throws Throwable {
                    return aNow(nextAction);
                }
            });
        }

        /**
         * Add next step to the sequence.
         *
         * @param nextAction the action that ignores the input argument
         * @param <N>        the next type
         * @return the sequence builder with next step
         */
        public <N> Promise<N> thenLastI(final ACallable<N> nextAction) {
            return thenI(nextAction).finish();
        }


        /**
         * Handle exception thrown by the previous steps.
         *
         * @param catcher the action that handles exceptions
         * @return the builder
         */
        public SeqBuilder<T> failed(final AFunction<T, Throwable> catcher) {
            return new SeqBuilder<T>(new ACallable<T>() {
                @Override
                public Promise<T> call() throws Throwable {
                    return aNow(action).mapOutcome(new AFunction<T, Outcome<T>>() {
                        @Override
                        public Promise<T> apply(final Outcome<T> value) throws Throwable {
                            if (value.isSuccess()) {
                                return Promise.forOutcome(value);
                            } else {
                                return catcher.apply(value.failure());
                            }
                        }
                    });
                }
            });
        }


        /**
         * Add next step to the sequence.
         *
         * @param mapper the mapper
         * @return the sequence builder with next step
         */
        public Promise<T> failedLast(final AFunction<T, Throwable> mapper) {
            return failed(mapper).finish();
        }


        /**
         * Execute action in any case and the end of sequence operator.
         * The result promise is resolves to the original result after
         * the finally body is resolved. Unless the original body has
         * a success outcome, but finally body fails.
         *
         * @param finallyAction an action to execute
         * @return a promise for the sequence result
         */
        public Promise<T> finallyDo(final ACallable<?> finallyAction) {
            final Promise<T> rc = new Promise<T>();
            final AResolver<T> resolver = rc.resolver();
            aNow(action).listen(new AResolver<T>() {
                @Override
                public void resolve(final Outcome<T> resolution) throws Throwable {
                    aNow(finallyAction).listen(new AResolver<Object>() {
                        @Override
                        public void resolve(final Outcome<Object> finallyResolution)
                                throws Throwable {
                            if (!finallyResolution.isSuccess() && resolution.isSuccess()) {
                                notifyResolver(resolver, ((Failure<Object>) finallyResolution).<T>toOtherType());
                            } else {
                                notifyResolver(resolver, resolution);
                            }
                        }
                    });
                }
            });
            return rc;
        }
    }

    /**
     * The seq for builder.
     *
     * @param <T> the element type
     */
    public static final class SeqForBuilder<T> {
        /**
         * THe loop body.
         */
        private final ACallable<OptionalValue<T>> body;

        /**
         * The constructor.
         *
         * @param body the body
         */
        public SeqForBuilder(final ACallable<OptionalValue<T>> body) {
            this.body = body;
        }

        /**
         * The sequential left fold for the collection.
         *
         * @param initial the initial value
         * @param folder  the folder
         * @param <R>     the result value
         * @return the folded value
         */
        public <R> Promise<R> leftFold(final R initial, final AFunction2<R, R, T> folder) {
            final Cell<R> result = new Cell<R>(initial);
            final AFunction<Boolean, OptionalValue<T>> foldStep = new AFunction<Boolean, OptionalValue<T>>() {
                @Override
                public Promise<Boolean> apply(final OptionalValue<T> value) throws Throwable {
                    if (!value.hasValue()) {
                        return aFalse();
                    } else {
                        return folder.apply(result.getValue(), value.value()).map(new AFunction<Boolean, R>() {
                            @Override
                            public Promise<Boolean> apply(final R value) throws Throwable {
                                result.setValue(value);
                                return aTrue();
                            }
                        });
                    }
                }
            };
            return aSeqLoop(new ACallable<Boolean>() {
                @Override
                public Promise<Boolean> call() throws Throwable {
                    return aNow(body).map(foldStep);
                }
            }).then(new ACallable<R>() {
                @Override
                public Promise<R> call() throws Throwable {
                    return aSuccess(result.getValue());
                }
            });
        }


        /**
         * Map the value.
         *
         * @param mapper the value mapper
         * @param <I>    the new produced type
         * @return the value
         */
        public <I> SeqForBuilder<I> map(final AFunction<I, T> mapper) {
            return new SeqForBuilder<I>(ProducerUtil.<I, T>mapProducer(body, mapper));
        }

        /**
         * The sequential right fold for the collection. It first collects elements using left fold, than does
         * a fold starting from the last element. This is slower version than left fold.
         *
         * @param initial the initial value
         * @param folder  the folder
         * @param <R>     the result value
         * @return the folded value
         */
        public <R> Promise<R> rightFold(final R initial, final AFunction2<R, R, T> folder) {
            final ArrayList<T> results = new ArrayList<T>();
            final Cell<R> result = new Cell<R>(initial);
            return leftFold(null, new AFunction2<Void, Void, T>() {
                @Override
                public Promise<Void> apply(final Void result, final T item) throws Throwable {
                    results.add(item);
                    return aVoid();
                }
            }).then(new ACallable<Void>() {
                @Override
                public Promise<Void> call() throws Throwable {
                    return aSeqLoop(new ACallable<Boolean>() {
                        @Override
                        public Promise<Boolean> call() throws Throwable {
                            if (results.isEmpty()) {
                                return aFalse();
                            } else {
                                final T nextElement = results.remove(results.size() - 1);
                                return folder.apply(result.getValue(), nextElement).map(new AFunction<Boolean, R>() {
                                    @Override
                                    public Promise<Boolean> apply(final R value) throws Throwable {
                                        result.setValue(value);
                                        return aTrue();
                                    }
                                });
                            }
                        }
                    });
                }
            }).then(new ACallable<R>() {
                @Override
                public Promise<R> call() throws Throwable {
                    return aSuccess(result.getValue());
                }
            });
        }

        /**
         * Reduce collection to a single item.
         *
         * @param reducer the reducer
         * @return the promise for result (returns option without value if there were no values)
         */
        public Promise<OptionalValue<T>> reduce(final AFunction2<T, T, T> reducer) {
            return leftFold(OptionalValue.<T>empty(), new AFunction2<OptionalValue<T>, OptionalValue<T>, T>() {
                @Override
                public Promise<OptionalValue<T>> apply(final OptionalValue<T> result, final T item) throws Throwable {
                    if (result.hasValue()) {
                        return reducer.apply(result.value(), item).map(new AFunction<OptionalValue<T>, T>() {
                            @Override
                            public Promise<OptionalValue<T>> apply(final T value) throws Throwable {
                                return aSuccess(OptionalValue.value(value));
                            }
                        });
                    } else {
                        return aSuccess(OptionalValue.value(item));
                    }
                }
            });
        }

        /**
         * @return the fold to unit value
         */
        public Promise<Void> toUnit() {
            return leftFold(null, new AFunction2<Void, Void, T>() {
                @Override
                public Promise<Void> apply(final Void result, final T item) throws Throwable {
                    return aVoid();
                }
            });
        }
    }
}
