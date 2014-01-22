package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.AsyncControl;
import net.sf.asyncobjects.core.Failure;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.ResolverUtil;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.data.Maybe;
import net.sf.asyncobjects.core.vats.Vat;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aFalse;
import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
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
     * Generic loop that executes body until it returns false.
     *
     * @param body the body to execute
     * @return the promises that resolves when body fails or returns true
     */
    public static Promise<Void> aSeqLoopFair(final ACallable<Boolean> body) {
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
     * Generic loop that executes body until it returns false. This is a greedy version of the loop,
     * that tries to fit as may iterations in the single vat turn as possible. This is more efficient,
     * but could be a problem, if there is no external limiting factor for the loop (like need for IO)
     * and loop is really long, as such loop will not allow others to execute. This version is best used
     * with IO operations over small objects. So as much as possible of IO buffers will be consumed, before
     * proceeding with the next action.
     *
     * @param body the body to execute
     * @return the promises that resolves when body fails or returns true
     */
    public static Promise<Void> aSeqLoop(final ACallable<Boolean> body) { // NOPMD
        final Promise<Void> result = new Promise<Void>();
        final AResolver<Void> resolver = result.resolver();
        final Cell<Promise<Void>> adjustedResult = new Cell<Promise<Void>>();
        ResolverUtil.notifySuccess(new AResolver<Boolean>() { // NOPMD

            @Override
            public void resolve(final Outcome<Boolean> resolution) throws Throwable {
                try {
                    Promise<Boolean> last = null;
                    Outcome<Boolean> outcome = resolution;
                    while (outcome != null && outcome.isSuccess() && outcome.value() != null && outcome.value()) {
                        last = aNow(body);
                        if (last.isResolved()) {
                            outcome = last.getOutcome();
                            last = null;
                        } else {
                            outcome = null;
                        }
                    }
                    if (last != null) {
                        // the delayed outcome
                        last.listen(this);
                        return;
                    }
                    outcome = outcomeMustBeFalse(outcome);
                    if (adjustedResult.isEmpty()) {
                        // all iterations completed.
                        adjustedResult.setValue(outcome.isSuccess() ? aVoid()
                                : AsyncControl.<Void>aFailure(outcome.failure()));
                    } else if (outcome.isSuccess()) {
                        ResolverUtil.notifySuccess(resolver, null);
                    } else {
                        ResolverUtil.notifyFailure(resolver, outcome.failure());
                    }
                } catch (Throwable t) {
                    if (adjustedResult.isEmpty()) {
                        adjustedResult.setValue(AsyncControl.<Void>aFailure(t));
                    } else {
                        ResolverUtil.notifyFailure(resolver, t);
                    }
                }
            }

            /**
             * Fix outcome in case if incorrect outcome is detected.
             *
             * @param srcOutcome the outcome
             * @return the fixed outcome
             */
            private Outcome<Boolean> outcomeMustBeFalse(final Outcome<Boolean> srcOutcome) {
                Outcome<Boolean> outcome = srcOutcome;
                if (outcome == null) {
                    outcome = Outcome.failure(new IllegalStateException("Outcome is null"));
                }
                if (outcome.isSuccess()) {
                    if (outcome.value() == null) {
                        outcome = Outcome.failure(new IllegalStateException("Outcome value is null"));
                    } else if (outcome.value()) {
                        outcome = Outcome.failure(new IllegalStateException("True must not happen at this point"));
                    }
                }
                return outcome;
            }
        }, true);
        if (adjustedResult.isEmpty()) {
            adjustedResult.setValue(result);
            return result;
        } else {
            return adjustedResult.getValue();
        }
    }

    /**
     * Generic loop that executes body until it returns non-empty value. The fair version
     * executes each step on own vat turn.
     *
     * @param body the body to execute
     * @param <A>  the result type
     * @return the promises that resolves when body fails or returns non-empty value
     */
    public static <A> Promise<A> aSeqMaybeLoopFair(final ACallable<Maybe<A>> body) {
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
     * Generic loop that executes body until it returns non-empty value. This is a greedy version of the loop.
     *
     * @param body the body to execute
     * @param <A>  the result type
     * @return the promises that resolves when body fails or returns non-empty value
     */
    public static <A> Promise<A> aSeqMaybeLoop(final ACallable<Maybe<A>> body) {
        final Cell<A> result = new Cell<A>();
        return aSeqLoop(new ACallable<Boolean>() {
            @Override
            public Promise<Boolean> call() throws Throwable {
                return body.call().map(new AFunction<Boolean, Maybe<A>>() {
                    @Override
                    public Promise<Boolean> apply(final Maybe<A> value) throws Throwable {
                        if (value.hasValue()) {
                            result.setValue(value.value());
                            return aFalse();
                        } else {
                            return aTrue();
                        }
                    }
                });
            }
        }).thenDo(new ACallable<A>() {
            @Override
            public Promise<A> call() throws Throwable {
                return aValue(result.getValue());
            }
        });
    }

    /**
     * Builder for the sequence of operations. Note that class may call all bodies on the single vat turn,
     * if they resolve immediately. So amount of bodies should be limited, in order prevent stack overflow.
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
        public <N> SeqBuilder<N> map(final AFunction<N, T> mapper) {
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
        public <N> Promise<N> mapLast(final AFunction<N, T> mapper) {
            return map(mapper).finish();
        }

        /**
         * Add next step to the sequence.
         *
         * @param nextAction the action that ignores the input argument
         * @param <N>        the next type
         * @return the sequence builder with next step
         */
        public <N> SeqBuilder<N> thenDo(final ACallable<N> nextAction) {
            return map(new AFunction<N, T>() {
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
        public <N> Promise<N> thenDoLast(final ACallable<N> nextAction) {
            return thenDo(nextAction).finish();
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
        @SuppressWarnings("unchecked")
        public Promise<T> finallyDo(final ACallable<?> finallyAction) {
            final Promise<T> current = aNow(action);
            if (current.isResolved()) {
                final Promise<Object> promise = (Promise<Object>) aNow(finallyAction);
                if (promise.isResolved()) {
                    if (promise.getOutcome().isSuccess() || !current.getOutcome().isSuccess()) {
                        return current;
                    } else {
                        return aFailure(promise.getOutcome().failure());
                    }
                } else {
                    return promise.mapOutcome(new AFunction<T, Outcome<Object>>() {
                        @Override
                        public Promise<T> apply(final Outcome<Object> value) throws Throwable {
                            if (value.isSuccess() || !current.getOutcome().isSuccess()) {
                                return current;
                            } else {
                                return aFailure(value.failure());
                            }
                        }
                    });
                }
            } else {
                final Promise<T> rc = new Promise<T>();
                final AResolver<T> resolver = rc.resolver();
                current.listen(new AResolver<T>() {
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
}
