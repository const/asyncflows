/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.core.util;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.ThreadSafe;
import org.asyncflows.core.context.Context;
import org.asyncflows.core.context.ContextKey;
import org.asyncflows.core.data.Subcription;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ARunner;
import org.asyncflows.core.function.ASupplier;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;

/**
 * Cancellation runner that fails if any running block has failed or this instance is failed.
 * The class is thread-safe and it could be passed around.
 */
@ThreadSafe
public class Cancellation implements ARunner {
    /**
     * Context key to be used with cancellation.
     */
    public static final ContextKey<Cancellation> CONTEXT_KEY = ContextKey.get(Cancellation.class);
    /**
     * Failure promise.
     */
    private final Promise<Void> failPromise = new Promise<>();
    /**
     * True, if failure promise is resolved already.
     */
    private final AtomicBoolean resolved = new AtomicBoolean(false);

    /**
     * @return a new cancellation runner that
     */
    public static Cancellation newCancellation() {
        return new Cancellation();
    }

    public static Cancellation currentOrNull() {
        return Context.current().getOrNull(CONTEXT_KEY);
    }

    @Override
    public <T> Promise<T> run(final ASupplier<T> action) {
        return run(action, null);
    }

    /**
     * Fail with problem.
     *
     * @param problem the problem
     */
    public void fail(final Throwable problem) {
        if (resolved.compareAndSet(false, true)) {
            failPromise.resolver().resolve(Outcome.failure(problem));
        }
    }

    /**
     * Cancel the operation.
     */
    public void cancel() {
        fail(new CancellationException());
    }

    /**
     * For scoped cancellable actions, use this method to indicate the end of the scope.
     */
    public void finish() {
        fail(new OutOfScopeException("cancellation"));
    }

    /**
     * Run action on cancel. The action is executed on arbitrary thread.
     *
     * @param action the action
     * @return a runnable that could be used to remove listener
     */
    public Subcription onCancelSync(Runnable action) {
        return onCancelSync(t -> action.run());
    }

    /**
     * Run action on cancel with exception. The action is executed on arbitrary thread.
     *
     * @param action the action
     * @return a runnable that could be used to remove listener
     */
    public Subcription onCancelSync(Consumer<Throwable> action) {
        final AResolver<Void> listener = o -> {
            if (o.isFailure()) {
                action.accept(o.failure());
            }
        };
        failPromise.listenSync(listener);
        return () -> failPromise.forget(listener);
    }

    /**
     * Run with cleanup.
     *
     * @param action  the action
     * @param cleanup the cleanup
     * @param <T>     the result type
     * @return the promise
     */
    public <T> Promise<T> run(final ASupplier<T> action, final Consumer<T> cleanup) {
        final Promise<T> localFailure = new Promise<>();
        final AResolver<Void> failureObserver = o -> {
            if (o.isFailure()) {
                localFailure.resolver().resolve(Outcome.failure(o.failure()));
            } else {
                localFailure.resolver().resolve(Outcome.failure(
                        new RuntimeException("BUG: Unexpected result: " + o)));
            }
        };
        failPromise.listenSync(failureObserver);
        if (!failPromise.isUnresolved()) {
            // if there is already failure, just return a promise that will eventually fail
            return localFailure;
        }
        final CoreFlowsAny.AnyBuilder<T> builder = CoreFlowsAny.aAny(
                () -> aNow(action).listen(o -> {
                    failPromise.forget(failureObserver);
                    if (o.isFailure()) {
                        fail(o.failure());
                    }
                })).or(promiseSupplier(localFailure));
        if (cleanup != null) {
            builder.suppressed(cleanup);
        }
        return builder.finish();
    }
}
