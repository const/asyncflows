package org.asyncflows.core;

import org.asyncflows.core.vats.SingleThreadVat;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;
import org.asyncflows.core.function.ARunner;
import org.asyncflows.core.function.ASupplier;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.asyncflows.core.AsyncControl.aLater;
import static org.asyncflows.core.AsyncControl.aNow;
import static org.asyncflows.core.AsyncControl.aResolver;

/**
 * Asynchronous context operations.
 */
public class AsyncContext {

    public static <T> Outcome<T> doAsyncOutcome(ASupplier<T> supplier) {
        final AtomicReference<Outcome<T>> outcome = new AtomicReference<>();
        final Object stopKey = new Object();
        final SingleThreadVat vat = new SingleThreadVat(stopKey);
        vat.execute(() -> aNow(supplier).listen(o -> {
            outcome.set(o);
            vat.stop(stopKey);
        }));
        vat.runInCurrentThread();
        return outcome.get();
    }

    public static <T> T doAsync(ASupplier<T> supplier) throws AsyncExecutionException {
        Outcome<T> outcome = doAsyncOutcome(supplier);
        if (outcome.isSuccess()) {
            return outcome.value();
        } else {
            throw new AsyncExecutionException(outcome.failure());
        }
    }

    public static <T> T doAsyncThrowable(ASupplier<T> supplier) throws Throwable {
        return doAsyncOutcome(supplier).force();
    }


    /**
     * Do in the default context.
     *
     * @param function the function that takes executor and runner
     * @param <R>      the runner
     * @return the result of action
     */
    public static <R> R withDefaultContext(BiFunction<ARunner, Executor, R> function) {
        final Vat current = Vat.currentOrNull();
        if (current != null) {
            return function.apply(AsyncControl::aNow, current);
        } else {
            final Vat vat = Vats.defaultVat();
            return function.apply(new ARunner() {
                @Override
                public <T> Promise<T> start(ASupplier<T> a) {
                    return aLater(a, vat);
                }
            }, vat);
        }
    }

    /**
     * Run action on the daemon executor and resolve promise when that action finishes.
     * This method is used when otherwise asynchronous component like SSLEngine requests
     * to execute some runnable.
     *
     * @param action the action to execute (the action will not have a vat context)
     * @return promise that resolves to the result of the execution
     */
    public static Promise<Void> aDaemonRun(final Runnable action) {
        return aResolver(resolver -> Vats.DAEMON_EXECUTOR.execute(() -> {
            try {
                action.run();
                Outcome.notifySuccess(resolver, null);
            } catch (Throwable t) {
                Outcome.notifyFailure(resolver, t);
            }
        }));
    }

    /**
     * Run action on the daemon executor and resolve promise when that action finishes.
     * This method is used when it is required to execute a single action on other thread
     * context w/o creating explicit vat. Usually it is used when interacting with non-asynchronous
     * components.
     *
     * @param action the action to execute (the action will not have a vat context)
     * @return promise that resolves to the result of the execution
     */
    public static <T> Promise<T> aDaemonGet(final Supplier<T> action) {
        return aResolver(resolver -> Vats.DAEMON_EXECUTOR.execute(() -> {
            try {
                Outcome.notifySuccess(resolver, action.get());
            } catch (Throwable t) {
                Outcome.notifyFailure(resolver, t);
            }
        }));
    }

}
