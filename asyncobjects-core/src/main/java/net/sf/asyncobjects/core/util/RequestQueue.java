package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Maybe;

import java.util.ArrayDeque;
import java.util.Deque;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoop;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqOptionLoop;

/**
 * The asynchronous request queue. It is is similar to non-reentrant mutex.
 */
public final class RequestQueue {
    /**
     * List of actions in the queue.
     */
    private final Deque<AResolver<Void>> queue = new ArrayDeque<AResolver<Void>>();
    /**
     * The resume resolver for the action promise.
     */
    private final AResolver<Object> resumeObserver = new AResolver<Object>() {
        @Override
        public void resolve(final Outcome<Object> resolution) throws Throwable {
            if (queue.isEmpty()) {
                running = false;
            } else {
                notifySuccess(queue.remove(), null);
            }
        }
    };
    /**
     * If {@link #suspend()} operation is in progress, this a resolver aht awakes it.
     */
    private AResolver<Void> suspendResolver;
    /**
     * If true, an action is currently running.
     */
    private boolean running;

    /**
     * Awake the {@link #suspend()} method.
     */
    public void resume() {
        if (suspendResolver != null) {
            notifySuccess(suspendResolver, null);
            suspendResolver = null;
        }
    }

    /**
     * @return the promise that resolves to void when someone calls {@link #resume()}
     */
    public Promise<Void> suspend() {
        if (suspendResolver != null) {
            throw new IllegalStateException("The suspend operation is already in the progress");
        }
        final Promise<Void> rc = new Promise<Void>();
        suspendResolver = rc.resolver();
        return rc;
    }

    /**
     * The form of suspend that resolves to true. It is used in loop, when loop should be continued immediately
     * after suspend operation. The method is the same as {@code requests.suspend().then(booleanCallable(true))},
     * but is slightly more optimized in order to resume cycle faster.
     *
     * @return the promise that resolves to true when someone calls {@link #resume()}
     */
    public Promise<Boolean> suspendThenTrue() {
        if (suspendResolver != null) {
            throw new IllegalStateException("The suspend operation is already in the progress");
        }
        final Promise<Boolean> rc = new Promise<Boolean>();
        final AResolver<Boolean> resolver = rc.resolver();
        suspendResolver = new AResolver<Void>() {
            @Override
            public void resolve(final Outcome<Void> resolution) throws Throwable {
                if (resolution.isSuccess()) {
                    notifySuccess(resolver, true);
                } else {
                    notifyFailure(resolver, resolution.failure());
                }
            }
        };
        return rc;
    }

    /**
     * The form of suspend that resolves to empty option. It is used in maybe loop, when loop should be
     * continued immediately  after suspend operation.
     *
     * @param <T> the body return type
     * @return the promise that resolves to empty option when someone calls {@link #resume()}
     */
    public <T> Promise<Maybe<T>> suspendThenEmpty() {
        if (suspendResolver != null) {
            throw new IllegalStateException("The suspend operation is already in the progress");
        }
        final Promise<Maybe<T>> rc = new Promise<Maybe<T>>();
        final AResolver<Maybe<T>> resolver = rc.resolver();
        suspendResolver = new AResolver<Void>() {
            @Override
            public void resolve(final Outcome<Void> resolution) throws Throwable {
                if (resolution.isSuccess()) {
                    notifySuccess(resolver, Maybe.<T>empty());
                } else {
                    notifyFailure(resolver, resolution.failure());
                }
            }
        };
        return rc;
    }


    /**
     * Run body on the request queue.
     *
     * @param body the body to run
     * @param <T>  the body return type
     * @return the promise that resolves when body finishes
     */
    public <T> Promise<T> run(final ACallable<T> body) {
        if (running) {
            final Promise<Void> blocker = new Promise<Void>();
            queue.addLast(blocker.resolver());
            return blocker.then(body).observe(resumeObserver);
        } else {
            running = true;
            return aNow(body).observe(resumeObserver);
        }
    }

    /**
     * Run a sequential loop inside the body. It is usually a loop with suspend.
     * Using request queue with such loop is one of the most common usage scenarios.
     *
     * @param body the loop body
     * @return the promise that resolve when loop finishes.
     */
    public Promise<Void> runSeqLoop(final ACallable<Boolean> body) {
        return run(new ACallable<Void>() {
            @Override
            public Promise<Void> call() throws Throwable {
                return aSeqLoop(body);
            }
        });
    }

    /**
     * Run a sequential option loop inside the body. It is usually a loop with suspend.
     * Using request queue with such loop is one of the common usage scenarios.
     *
     * @param body the loop body
     * @param <T>  the body return type
     * @return the promise that resolve when loop finishes.
     */
    public <T> Promise<T> runSeqOptionLoop(final ACallable<Maybe<T>> body) {
        return run(new ACallable<T>() {
            @Override
            public Promise<T> call() throws Throwable {
                return aSeqOptionLoop(body);
            }
        });
    }
}
