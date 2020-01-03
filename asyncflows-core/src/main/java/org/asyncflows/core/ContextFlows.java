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

package org.asyncflows.core;

import org.asyncflows.core.context.Context;
import org.asyncflows.core.data.Subcription;
import org.asyncflows.core.function.AFunction;
import org.asyncflows.core.function.ASupplier;

import java.util.Objects;
import java.util.function.UnaryOperator;

import static org.asyncflows.core.CoreFlows.aNow;

/**
 * Asynchronous operations related to {@link Context}.
 */
public class ContextFlows {

    /**
     * Private constructor for utility class.
     */
    private ContextFlows() {
    }

    /**
     * Run operation with specified context.
     *
     * @param context the context
     * @param action  the action
     * @param <R>     the result.
     * @return the result of action execution.
     */
    public static <R> Promise<R> inContext(Context context, ASupplier<R> action) {
        try (Subcription ignored = context.setContext()) {
            return aNow(action);
        }
    }

    /**
     * Run operation with transformed context.
     *
     * @param transform the context transform
     * @param action    the action
     * @param <R>       the result.
     * @return the result of action execution.
     */
    public static <R> Promise<R> inContext(UnaryOperator<Context> transform, ASupplier<R> action) {
        return inContext(Context.current().transform(transform), action);
    }

    /**
     * Run operation in the context that is calculate asynchronously.
     * <pre>{@code
     *    return inContext(c -> aValue(c.transform(withMdcEntry("k", "v")))).run(() -> aLater(() -> {
     *         assertEquals("v", MDC.get("k"));
     *         return aVoid();
     *    }));
     * }</pre>
     *
     * @param asyncTransform the asynchronous context function
     * @return the builder for operations
     */
    public static InContextBuilder inContext(AFunction<Context, Context> asyncTransform) {
        return inContext(() -> Objects.requireNonNull(asyncTransform, "asyncTransform").apply(Context.current()));
    }

    /**
     * Run operation in the context that is calculate asynchronously.
     * <pre>{@code
     *    return inContext(() -> aValue(Context.empty().transform(withMdcEntry("k", "v")))).run(() -> aLater(() -> {
     *         assertEquals("v", MDC.get("k"));
     *         return aVoid();
     *    }));
     * }</pre>
     *
     * @param contextSupplier the asynchronous context supplier
     * @return the builder for operations
     */
    public static InContextBuilder inContext(ASupplier<Context> contextSupplier) {
        return new InContextBuilder(contextSupplier);
    }

    /**
     * The builder to support class.
     */
    public static final class InContextBuilder {
        /**
         * The transform action for the context
         */
        private final ASupplier<Context> contextSupplier;

        /**
         * The private constructor.
         *
         * @param contextSupplier the supplier
         */
        private InContextBuilder(ASupplier<Context> contextSupplier) {
            this.contextSupplier = contextSupplier;
        }

        /**
         * Run action in the context.
         *
         * @param action the action
         * @param <R>    the result type
         * @return a promise for result
         */
        public <R> Promise<R> run(ASupplier<R> action) {
            return aNow(contextSupplier).flatMap(c -> inContext(c, action));
        }
    }
}
