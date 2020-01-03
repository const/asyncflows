/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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

import org.asyncflows.core.Promise;
import org.asyncflows.core.context.Context;
import org.asyncflows.core.data.Subcription;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.function.AsyncFunctionUtil;
import org.asyncflows.core.function.FunctionExporter;

import java.util.function.Consumer;

import static org.asyncflows.core.ContextFlows.inContext;
import static org.asyncflows.core.CoreFlows.aNow;

/**
 * Cancellable flow utilities.
 */
public final class CancellableFlows {

    /**
     * Private constructor.
     */
    private CancellableFlows() {
        // do nothing
    }

    /**
     * Execute action in a new cancellation context not linked to previous one.
     *
     * @param action the action to be executed.
     * @param <R>    the result type
     * @return the action result
     */
    public static <R> Promise<R> aWithNewCancellation(ASupplier<R> action) {
        return aWithNewCancellation(AsyncFunctionUtil.supplierToFunction(action));
    }

    /**
     * Execute action in a new cancellation context not linked to previous one.
     *
     * @param action the action to be executed.
     * @param <R>    the result type
     * @return the action result
     */
    public static <R> Promise<R> aWithNewCancellation(AFunction<Cancellation, R> action) {
        final Cancellation cancellation = Cancellation.newCancellation();
        return aWithCancellation(cancellation, action).listenSync(o -> cancellation.finish());
    }

    /**
     * Execute action in the local cancellable. A new cancellation is created, and if outer cancellation
     * is cancelled, this is canceled too, but reverse is not true, if this cancellation is cancelled, outer
     * is not cancelled.
     *
     * @param action the action to be executed.
     * @param <R>    the result type
     * @return the action result
     */
    public static <R> Promise<R> aWithLocalCancellation(AFunction<Cancellation, R> action) {
        final Cancellation cancellation = Cancellation.newCancellation();
        final Subcription cleanup = registerCurrentCancellableListener(cancellation::fail);
        return aWithCancellation(cancellation, action).listenSync(o -> {
            try {
                cleanup.close();
            } finally {
                cancellation.finish();
            }
        });
    }

    /**
     * Execute action in the local cancellable. A new cancellation is created, and if outer cancellation
     * is cancelled, this is canceled too, but reverse is not true, if this cancellation is cancelled, outer
     * is not cancelled.
     *
     * @param action the action to be executed.
     * @param <R>    the result type
     * @return the action result
     */
    public static <R> Promise<R> aWithLocalCancellation(ASupplier<R> action) {
        return aWithLocalCancellation(AsyncFunctionUtil.supplierToFunction(action));
    }

    /**
     * Register listener on the current cancellable if it exists
     *
     * @param ca the cancellable action
     * @return action to be executed to remove cancelable installation
     */
    private static Subcription registerCurrentCancellableListener(Consumer<Throwable> ca) {
        return registerCurrentCancellableListener(Context.current(), ca);
    }

    /**
     * Register listener on the current cancellable if it exists
     *
     * @param cancellation the cancellation
     * @param ca           the cancellable action
     * @return action to be executed to remove cancelable installation
     */
    private static Subcription registerCurrentCancellableListener(Context cancellation, Consumer<Throwable> ca) {
        return cancellation.getOrEmpty(Cancellation.CONTEXT_KEY)
                .map(c -> c.onCancelSync(ca))
                .orElse(Subcription.noCleanup());
    }

    /**
     * Execute action in the specified cancellation context.
     *
     * @param cancellation the cancellation to use in context
     * @param action       the action to be executed.
     * @param <R>          the result type
     * @return the action result
     */
    public static <R> Promise<R> aWithCancellation(Cancellation cancellation, ASupplier<R> action) {
        return inContext(c -> c.with(Cancellation.CONTEXT_KEY, cancellation), action);
    }

    /**
     * Execute action in the specified cancellation context.
     *
     * @param cancellation the cancellation to use in context
     * @param action       the action to be executed.
     * @param <R>          the result type
     * @return the action result
     */
    public static <R> Promise<R> aWithCancellation(Cancellation cancellation, AFunction<Cancellation, R> action) {
        return inContext(c -> c.with(Cancellation.CONTEXT_KEY, cancellation), () -> action.apply(cancellation));
    }

    /**
     * The non-cancellable. This basically executes block with removed cancellation context.
     *
     * @param action the action to be executed.
     * @param <R>    the result type
     * @return the action result
     */
    public static <R> Promise<R> aNonCancellable(ASupplier<R> action) {
        return inContext(c -> c.without(Cancellation.CONTEXT_KEY), action);
    }

    /**
     * Cancellable action. The action itself might execute with cancellation or not. So {@code onCancel()} variants
     * are invoked only if there is an actual cancellation.
     *
     * @param action the action
     * @param <R>    the result
     * @return builder for action
     */
    public static <R> CancellableBuilder<R> aCancellable(ASupplier<R> action) {
        return new CancellableBuilder<>(action);
    }

    /**
     * The builder for cancellable actions.
     *
     * @param <R> the result type
     */
    public static class CancellableBuilder<R> {
        /**
         * The action to be executed.
         */
        private final ASupplier<R> action;

        /**
         * A constructor.
         *
         * @param action the action
         */
        private CancellableBuilder(ASupplier<R> action) {
            this.action = action;
        }

        /**
         * The execute construct with {@code onCancelAction} action executed when cancellation happen.
         * The action {@code onCancelAction} is executed in arbitrary thread context w/o context propagation.
         *
         * @param onCancelAction the action executed on the cancel.
         */
        public Promise<R> onCancelSync(Consumer<Throwable> onCancelAction) {
            final Subcription subcription = registerCurrentCancellableListener(onCancelAction);
            return aNow(action).listenSync(o -> subcription.close());
        }

        /**
         * The execute construct with {@code onCancelAction} action executed when cancellation happen.
         *
         * @param onCancelAction the action executed on the cancel.
         */
        public Promise<R> onCancel(Consumer<Throwable> onCancelAction) {
            final Context current = Context.current();
            return onCancelSync(FunctionExporter.exportConsumer(t -> {
                try (final Subcription ignored = current.setContext()) {
                    onCancelAction.accept(t);
                }
            }));
        }
    }
}
