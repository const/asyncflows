package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Failure;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.ResolverUtil;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
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
                try {
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
                } catch (Throwable t) {
                    ResolverUtil.notifyFailure(resolver, t);
                }
            }
        }, true);
        return result;
    }

    /**
     * Generic loop that executes body until it returns non-empty value.
     *
     * @param body the body to execute
     * @param <A>  the result type
     * @return the promises that resolves when body fails or returns non-empty value
     */
    public static <A> Promise<A> aSeqOptionLoop(final ACallable<Maybe<A>> body) {
        final Promise<A> result = new Promise<A>();
        final AResolver<A> resolver = result.resolver();
        final Vat vat = Vat.current();
        ResolverUtil.notifySuccess(new AResolver<Maybe<A>>() {
            private final AResolver<Maybe<A>> self = this;

            @Override
            public void resolve(final Outcome<Maybe<A>> resolution) throws Throwable {
                try {
                    if (resolution.isSuccess()) {
                        if (resolution.value().isEmpty()) {
                            vat.execute(vat, new Runnable() {
                                @Override
                                public void run() {
                                    aNow(body).listen(self);
                                }
                            });
                        } else {
                            ResolverUtil.notifySuccess(resolver, resolution.value().value());
                        }
                    } else {
                        ResolverUtil.notifyFailure(resolver, resolution.failure());
                    }
                } catch (Throwable t) {
                    ResolverUtil.notifyFailure(resolver, t);
                }
            }
        }, Maybe.<A>empty());
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
}
