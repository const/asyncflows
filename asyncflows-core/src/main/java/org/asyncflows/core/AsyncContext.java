/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

package org.asyncflows.core;

import org.asyncflows.core.function.ARunner;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.vats.SingleThreadVat;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.core.vats.Vats;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aNow;
import static org.asyncflows.core.CoreFlows.aResolver;

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
    public static <R> R withDefaultContext(BiFunction<ARunner, Vat, R> function) {
        final Vat current = Vat.currentOrNull();
        if (current != null) {
            return function.apply(CoreFlows::aNow, current);
        } else {
            final Vat vat = Vats.defaultVat();
            return function.apply(new ARunner() {
                @Override
                public <T> Promise<T> run(ASupplier<T> a) {
                    return aLater(vat, a);
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
     * @param <T>    the type
     * @return promise that resolves to the result of the execution
     */
    public static <T> Promise<T> aDaemonGet(final Supplier<T> action) {
        return aExecutorGet(action, Vats.DAEMON_EXECUTOR);
    }

    /**
     * Get value using executor.
     *
     * @param action   the action
     * @param executor the executor
     * @param <T>      the type
     * @return the value
     */
    public static <T> Promise<T> aExecutorGet(Supplier<T> action, ExecutorService executor) {
        return aResolver(resolver -> {
            executor.execute(() -> {
                try {
                    Outcome.notifySuccess(resolver, action.get());
                } catch (Throwable t) {
                    Outcome.notifyFailure(resolver, t);
                }
            });
        });
    }

    /**
     * Run action on the ForkJoin executor and resolve promise when that action finishes.
     * This method is used when it is required to execute a single action on other thread
     * context w/o creating explicit vat. Usually it is used when interacting with non-asynchronous
     * components.
     *
     * @param action the action to execute (the action will not have a vat context)
     * @param <T>    the type
     * @return promise that resolves to the result of the execution
     */
    public static <T> Promise<T> aForkJoinGet(final Supplier<T> action) {
        return aExecutorGet(action, ForkJoinPool.commonPool());
    }


}
