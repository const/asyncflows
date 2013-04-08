package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AResolver;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;

import java.util.Deque;
import java.util.LinkedList;

import static net.sf.asyncobjects.core.AsyncControl.aNow;
import static net.sf.asyncobjects.core.ResolverUtil.notifyFailure;
import static net.sf.asyncobjects.core.ResolverUtil.notifySuccess;

/**
 * The asynchronous request queue. It is is similar to non-reentrant mutex.
 */
public final class RequestQueue {
    /**
     * If {@link #suspend()} operation is in progress, this a resolver aht awakes it.
     */
    private AResolver<Void> suspendResolver;
    /**
     * If true, an action is currently running.
     */
    private boolean running;
    /**
     * List of actions in the queue.
     */
    private final Deque<AResolver<Void>> queue = new LinkedList<AResolver<Void>>();
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
     * Awake the {@link #suspend()} method.
     */
    public void awake() {
        if (suspendResolver != null) {
            notifySuccess(suspendResolver, null);
            suspendResolver = null;
        }
    }


    /**
     * @return the promise that resolves to void when someone calls {@link #awake()}
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
     * @return the promise that resolves to true when someone calls {@link #awake()}
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
}
