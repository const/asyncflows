package org.asyncflows.core.util;

import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ARunner;
import org.asyncflows.core.function.ASupplier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.function.AsyncFunctionUtil.promiseSupplier;

/**
 * Fail-fast runner that fails if any running block has failed.
 */
public class FailFast implements ARunner {
    /**
     * Failure promise.
     */
    private final Promise<Void> failPromise = new Promise<>();
    /**
     * True, if failure promise is resolved already.
     */
    private final AtomicBoolean resolved = new AtomicBoolean(false);

    /**
     * @return a fail-fast runner that
     */
    public static FailFast failFast() {
        return new FailFast();
    }

    @Override
    public <T> Promise<T> run(ASupplier<T> action) {
        return run(action, null);
    }

    /**
     * Run with cleanup.
     *
     * @param action  the action
     * @param cleanup the cleanup
     * @param <T>     the result type
     * @return the promise
     */
    public <T> Promise<T> run(ASupplier<T> action, Consumer<T> cleanup) {
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
        CoreFlowsAny.AnyBuilder<T> builder = CoreFlowsAny.aAny(
                () -> aNow(action).listen(o -> {
                    failPromise.forget(failureObserver);
                    if (o.isFailure()) {
                        if (resolved.compareAndSet(false, true)) {
                            failPromise.resolver().resolve(Outcome.failure(o.failure()));
                        }
                    }
                })).or(promiseSupplier(localFailure));
        if (cleanup != null) {
            builder.suppressed(cleanup);
        }
        return builder.finish();
    }
}
