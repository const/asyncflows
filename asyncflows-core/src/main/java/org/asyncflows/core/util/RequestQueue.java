package org.asyncflows.core.util;


import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The asynchronous request queue. It is is similar to non-reentrant mutex.
 */
public final class RequestQueue {
    /**
     * List of actions in the queue.
     */
    private final Deque<AResolver<Void>> queue = new ArrayDeque<>();
    /**
     * If {@link #suspend()} operation is in progress, this a resolver aht awakes it.
     */
    private AResolver<Void> suspendResolver;
    /**
     * If true, an action is currently running.
     */
    private boolean running;
    /**
     * The resume resolver for the action promise.
     */
    private final AResolver<Object> resumeObserver = resolution -> {
        if (queue.isEmpty()) {
            running = false;
        } else {
            notifySuccess(queue.remove(), null);
        }
    };

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
        final Promise<Void> rc = new Promise<>();
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
        final Promise<Boolean> rc = new Promise<>();
        final AResolver<Boolean> resolver = rc.resolver();
        suspendResolver = resolution -> {
            if (resolution.isSuccess()) {
                notifySuccess(resolver, true);
            } else {
                notifyFailure(resolver, resolution.failure());
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
        final Promise<Maybe<T>> rc = new Promise<>();
        final AResolver<Maybe<T>> resolver = rc.resolver();
        suspendResolver = resolution -> {
            if (resolution.isSuccess()) {
                notifySuccess(resolver, Maybe.<T>empty());
            } else {
                notifyFailure(resolver, resolution.failure());
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
    public <T> Promise<T> run(final ASupplier<T> body) {
        if (running) {
            final Promise<Void> blocker = new Promise<>();
            queue.addLast(blocker.resolver());
            return blocker.thenFlatGet(body).listen(resumeObserver);
        } else {
            running = true;
            return aNow(body).listen(resumeObserver);
        }
    }

    /**
     * Run a sequential loop inside the body. It is usually a loop with suspend.
     * Using request queue with such loop is one of the most common usage scenarios.
     *
     * @param body the loop body
     * @return the promise that resolve when loop finishes.
     */
    public Promise<Void> runSeqWhile(final ASupplier<Boolean> body) {
        return run(() -> aSeqWhile(body));
    }

    /**
     * Run a sequential option loop inside the body. It is usually a loop with suspend.
     * Using request queue with such loop is one of the common usage scenarios.
     *
     * @param body the loop body
     * @param <T>  the body return type
     * @return the promise that resolve when loop finishes.
     */
    public <T> Promise<T> runSeqUntilValue(final ASupplier<Maybe<T>> body) {
        return run(() -> aSeqUntilValue(body));
    }

    /**
     * @return true if some request is active on the queue.
     */
    public boolean isRunning() {
        return running;
    }
}
