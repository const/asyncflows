package net.sf.asyncobjects.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.CoreFunctionUtil.booleanCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.constantCallable;
import static net.sf.asyncobjects.core.CoreFunctionUtil.evaluate;
import static net.sf.asyncobjects.core.CoreFunctionUtil.promiseCallable;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;

/**
 * The promise. The class is intentionally not a thread-safe,
 * and it should be used only in the vat that has created it.
 * Other vats should have own promises that interact with
 * this using resolvers and listeners.
 *
 * @param <T> the value type
 */
public final class Promise<T> {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Promise.class);
    /**
     * The promise state.
     */
    private State state = State.INITIAL;
    /**
     * The outcome.
     */
    private Outcome<T> outcome;
    /**
     * The head of listener list.
     */
    private ListenerCell<T> listenersHead;
    /**
     * The tail of listener list.
     */
    private ListenerCell<T> listenersTail;

    /**
     * The success promise.
     *
     * @param value the value
     * @param <T>   the value type
     * @return the promise resolved to the specified outcome
     */
    public static <T> Promise<T> success(final T value) {
        return forOutcome(new Success<T>(value));
    }

    /**
     * The failure promise.
     *
     * @param problem the problem
     * @param <T>     the value type
     * @return the promise resolved to the failure
     */
    public static <T> Promise<T> failure(final Throwable problem) {
        return forOutcome(new Failure<T>(problem));
    }

    /**
     * The promise with the specified outcome.
     *
     * @param outcome outcome
     * @param <T>     the value type
     * @return the promise
     */
    public static <T> Promise<T> forOutcome(final Outcome<T> outcome) {
        final Promise<T> promise = new Promise<T>();
        promise.state = State.RESOLVED;
        promise.outcome = outcome;
        return promise;
    }

    /**
     * @return the state of the promise
     */
    public State getState() {
        return state;
    }

    /**
     * @return true if the promise is resolved
     */
    public boolean isResolved() {
        return state == State.RESOLVED;
    }

    /**
     * @return the outcome of the promise
     */
    public Outcome<T> getOutcome() {
        if (state != State.RESOLVED) {
            throw new IllegalStateException("There is no outcome yet! State = " + state);
        }
        return outcome;
    }

    /**
     * @return the resolver for the promise, it could be got only once
     */
    public AResolver<T> resolver() {
        return CoreExportUtil.export(shortcutResolver());
    }

    /**
     * This a dangerous method that might cause stack overflow. It avoids sending an resolution event in the case
     * of chained map and thenDo calls. The optimization will be possible removed in the future.
     *
     * @return the resolver for the promise, it could be got only once
     */
    private AResolver<T> shortcutResolver() {
        if (state != State.INITIAL) {
            throw new IllegalStateException("Resolver is already got: " + state);
        }
        state = State.RESOLVING;
        return new AResolver<T>() {
            @Override
            public void resolve(final Outcome<T> resolution) throws Throwable {
                if (state == State.RESOLVING) {
                    outcome = resolution;
                    state = State.RESOLVED;
                    for (ListenerCell<T> current = listenersHead; current != null; current = current.next) {
                        ResolverUtil.notifyResolver(current.listener, resolution);
                    }
                    listenersHead = null;
                    listenersTail = null;
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Resolving promise in invalid state: " + state);
                    }
                }
            }
        };
    }

    /**
     * Execute action after this promise finishes successfully. It is used instead of {@link #map(AFunction)},
     * when the result of executing this promise is not needed for the next action (for example if it is
     * a promise for {@link Void}}), but it still should be known whether the operation fails or succeed.
     *
     * @param action the asynchronous mapper
     * @param <X>    the result type
     * @return the mapped promise
     */
    public <X> Promise<X> thenDo(final ACallable<X> action) {
        if (isResolved()) {
            if (outcome.isSuccess()) {
                return aNow(action);
            } else {
                return failure(outcome.failure());
            }
        } else {
            final Promise<X> promise = new Promise<X>();
            final AResolver<X> resolver = promise.shortcutResolver();
            listen(new AResolver<T>() {
                @Override
                public void resolve(final Outcome<T> resolution) throws Throwable {
                    if (resolution.isSuccess()) {
                        aNow(action).listen(resolver);
                    } else {
                        notifyFailure(resolver, resolution.failure());
                    }
                }
            });
            return promise;
        }
    }

    /**
     * Return promise that resolves to the specified value after current promise resolves.
     *
     * @param value the value
     * @param <X>   the value type
     * @return the promise
     */
    public <X> Promise<X> thenValue(final X value) {
        return thenDo(constantCallable(value));
    }

    /**
     * Return promise that resolves to the specified value after current promise resolves.
     * This is a specialized value for booleans, because it booleans happen a lot, and this particular
     * versions creates less objects.
     *
     * @param value the value
     * @return the promise
     */
    public Promise<Boolean> thenValue(final boolean value) {
        return thenDo(booleanCallable(value));
    }

    /**
     * Return promise that resolves to the specified value after current promise resolves.
     *
     * @param promise the value
     * @param <X>     the value type
     * @return the promise
     */
    public <X> Promise<X> thenPromise(final Promise<X> promise) {
        return thenDo(promiseCallable(promise));
    }

    /**
     * If the current promise completes with success, then fail result with provided failure.
     *
     * @param failure the failure
     * @param <X>     the failure type
     * @return the failure
     */
    public <X> Promise<X> thenFailure(final Throwable failure) {
        return thenDo(CoreFunctionUtil.<X>failureCallable(failure));
    }

    /**
     * Map the promise. In case of the failure, the body is not called.
     *
     * @param mapper the asynchronous mapper
     * @param <X>    the result type
     * @return the mapped promise
     */
    public <X> Promise<X> map(final AFunction<X, T> mapper) {
        if (isResolved()) {
            if (outcome.isSuccess()) {
                return evaluate(outcome.value(), mapper);
            } else {
                return failure(outcome.failure());
            }
        } else {
            final Promise<X> promise = new Promise<X>();
            final AResolver<X> resolver = promise.shortcutResolver();
            listen(new AResolver<T>() {
                @Override
                public void resolve(final Outcome<T> resolution) throws Throwable {
                    try {
                        mapper.apply(resolution.force()).listen(resolver);
                    } catch (Throwable t) {
                        ResolverUtil.notifyResolver(resolver, new Failure<X>(t));
                    }
                }
            });
            return promise;
        }
    }

    /**
     * Map outcome the promise. In case of the failure, the body is still called with the outcome.
     *
     * @param mapper the asynchronous mapper
     * @param <X>    the result type
     * @return the mapped promise
     */
    public <X> Promise<X> mapOutcome(final AFunction<X, Outcome<T>> mapper) {
        if (isResolved()) {
            return evaluate(outcome, mapper);
        } else {
            final Promise<X> promise = new Promise<X>();
            final AResolver<X> resolver = promise.shortcutResolver();
            listen(new AResolver<T>() {
                @Override
                public void resolve(final Outcome<T> resolution) throws Throwable {
                    try {
                        mapper.apply(resolution).listen(resolver);
                    } catch (Throwable t) {
                        ResolverUtil.notifyResolver(resolver, new Failure<X>(t));
                    }
                }
            });
            return promise;
        }
    }

    /**
     * @return to unit promise
     */
    public Promise<Void> toVoid() {
        if (state == State.RESOLVED) {
            return outcome.isSuccess() ? aVoid() : Promise.<Void>failure(outcome.failure());
        }
        return map(CoreFunctionUtil.<T>voidMapper());
    }

    /**
     * @return create outcome promise from this promise
     */
    public Promise<Outcome<T>> toOutcomePromise() {
        if (state == State.RESOLVED) {
            return success(outcome);
        } else {
            return mapOutcome(new AFunction<Outcome<T>, Outcome<T>>() {
                @Override
                public Promise<Outcome<T>> apply(final Outcome<T> value) throws Throwable {
                    return success(value);
                }
            });
        }
    }

    /**
     * Add listener or immediately resolve it in the call.
     *
     * @param resolver the resolver that will listen for promises
     */
    public void listen(final AResolver<? super T> resolver) {
        if (state == State.RESOLVED) {
            ResolverUtil.notifyResolver(resolver, outcome);
        } else {
            final ListenerCell<T> listener = new ListenerCell<T>(resolver);
            if (listenersTail != null) {
                listenersTail.next = listener;
                listenersTail = listener;
            } else {
                listenersHead = listener;
                listenersTail = listener;
            }
        }
    }

    /**
     * Add listener to promise and return it.
     *
     * @param resolver the resolver
     * @return this promise
     */
    public Promise<T> observe(final AResolver<? super T> resolver) {
        listen(resolver);
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Promise{");
        if (state == State.RESOLVED) {
            sb.append(outcome);
        } else {
            sb.append(state);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * The promise state.
     */
    public enum State {
        /**
         * Initial state.
         */
        INITIAL,
        /**
         * The being resolved state.
         */
        RESOLVING,
        /**
         * The resolved state.
         */
        RESOLVED
    }

    /**
     * The listener cell in the listener list.
     *
     * @param <A> the resolver type
     */
    private static final class ListenerCell<A> {
        /**
         * The wrapped listener.
         */
        private final AResolver<? super A> listener;
        /**
         * The next listener.
         */
        private ListenerCell<A> next;

        /**
         * The constructor from resolver.
         *
         * @param resolver the resolver
         */
        private ListenerCell(final AResolver<? super A> resolver) {
            this.listener = resolver;
        }
    }
}
