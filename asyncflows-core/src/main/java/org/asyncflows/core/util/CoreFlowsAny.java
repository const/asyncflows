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
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.vats.Vat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aNow;

/**
 * The control flow for ANY.
 */
public final class CoreFlowsAny {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CoreFlowsAny.class);

    /**
     * The private constructor for utility class.
     */
    private CoreFlowsAny() {
        // do nothing
    }

    /**
     * Start building Any block.
     *
     * @param firstAction the first action
     * @param <T>         the type
     * @return the builder
     */
    public static <T> AnyBuilder<T> aAny(final ASupplier<T> firstAction) {
        return aAny(false, firstAction);
    }

    /**
     * Start building any operator.
     *
     * @param preferSuccess if success is preferred, the success result will return if available
     *                      even if there were failures previously
     * @param firstAction   the first action for builder
     * @param <T>           the result.
     * @return the builder
     */
    public static <T> AnyBuilder<T> aAny(final boolean preferSuccess, final ASupplier<T> firstAction) {
        return new AnyBuilder<T>(preferSuccess).or(firstAction);
    }


    /**
     * The builder for aAny().
     *
     * @param <T> the result type.
     */
    public static class AnyBuilder<T> {
        /**
         * If true, success results have higher priority than failure results.
         */
        private final boolean preferSuccess;
        /**
         * The result promise.
         */
        private Promise<T> promise = new Promise<>();
        /**
         * The resolver for result promise.
         */
        private AResolver<T> resolver = promise.resolver();
        /**
         * The suppressed result handler.
         */
        private Consumer<T> suppressedResultHandler;
        /**
         * The suppressed failure handler.
         */
        private Consumer<Throwable> suppressedFailureHandler;
        /**
         * Branch count.
         */
        private int count;
        /**
         * The error outcome if available.
         */
        private Outcome<T> error;

        /**
         * The constructor.
         *
         * @param preferSuccess true if successful results are preferred.
         */
        public AnyBuilder(final boolean preferSuccess) {
            Vat.current(); // ensure asynchronous context
            this.preferSuccess = preferSuccess;
        }

        /**
         * Transform method, that allows grouping some operations.
         *
         * @param body the body
         * @param <R>  the result type
         * @return the result
         */
        public <R> R transform(Function<AnyBuilder<T>, R> body) {
            Objects.requireNonNull(body);
            return body.apply(this);
        }

        /**
         * Add new branch.
         *
         * @param action an action
         * @return this builder
         */
        @SuppressWarnings("squid:S3776")
        public AnyBuilder<T> or(final ASupplier<T> action) {
            if (promise == null) {
                throw new IllegalStateException("Action is already started");
            }
            count++;
            aNow(action).listen(o -> {
                count--;
                if (resolver == null) {
                    if (o.isFailure()) {
                        if (suppressedFailureHandler != null) {
                            suppressedFailureHandler.accept(o.failure());
                        }
                    } else {
                        if (suppressedResultHandler != null) {
                            suppressedResultHandler.accept(o.value());
                        }
                    }
                } else if (o.isSuccess()) {
                    resolver.resolve(o);
                    resolver = null;
                    if (error != null) {
                        if (suppressedFailureHandler != null) {
                            try {
                                suppressedFailureHandler.accept(error.failure());
                            } catch (Throwable t) {
                                LOG.debug("Failed to notify suppressed block", t);
                            }
                        }
                        error = null;
                    }
                } else if (preferSuccess) {
                    if (error == null) {
                        error = o;
                    } else if (error.failure() != o.failure()) {
                        error.failure().addSuppressed(o.failure());
                    }
                    if (count == 0) {
                        resolver.resolve(error);
                        error = null;
                        resolver = null;
                    }
                } else {
                    resolver.resolve(o);
                    resolver = null;
                }
            });
            return this;
        }

        /**
         * Add branch and finish building.
         *
         * @param action the action
         * @return the result promise
         */
        public Promise<T> orLast(final ASupplier<T> action) {
            return or(action).finish();
        }

        /**
         * Set suppressed result handler.
         *
         * @param handlerBody the result handler
         * @return this builder.
         */
        public AnyBuilder<T> suppressed(final Consumer<T> handlerBody) {
            if (this.suppressedResultHandler != null) {
                throw new IllegalStateException("Handler is already set");
            }
            this.suppressedResultHandler = handlerBody;
            return this;
        }


        /**
         * Set suppressed result handler and finish.
         *
         * @param handlerBody the result handler
         * @return this builder.
         */
        public Promise<T> suppressedLast(final Consumer<T> handlerBody) {
            return suppressed(handlerBody).finish();
        }

        /**
         * Set suppressed failure handler.
         *
         * @param handlerBody the failure
         * @return the builder
         */
        public AnyBuilder<T> suppressedFailure(final Consumer<Throwable> handlerBody) {
            if (this.suppressedFailureHandler != null) {
                throw new IllegalStateException("Handler is already set");
            }
            this.suppressedFailureHandler = handlerBody;
            return this;
        }

        /**
         * Set suppressed failure handler and finish.
         *
         * @param handlerBody the failure
         * @return the builder
         */
        public Promise<T> suppressedFailureLast(final Consumer<Throwable> handlerBody) {
            return suppressedFailure(handlerBody).finish();
        }

        /**
         * Finish building.
         *
         * @return the result promise
         */
        public Promise<T> finish() {
            if (count == 0) {
                return aFailure(new IllegalStateException("At least one alternative needs to be supplied"));
            }
            final Promise<T> p = promise;
            promise = null;
            return p;
        }
    }
}
