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

import org.asyncflows.core.function.AResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The outcome type.
 *
 * @param <T> the value type
 */
public abstract class Outcome<T> {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Outcome.class);

    /**
     * Upcast outcome value to a weaker type.
     *
     * @param input the input outcome
     * @param <O>   the output type
     * @param <I>   the input type
     * @return Upcasted the outcome
     */
    @SuppressWarnings("unchecked")
    public static <O, I extends O> Outcome<O> upcast(final Outcome<I> input) {
        return (Outcome<O>) input;
    }

    /**
     * Create success outcome.
     *
     * @param value the value
     * @param <A>   the value type
     * @return the success outcome
     */
    public static <A> Outcome<A> success(final A value) {
        return new Success<>(value);
    }

    /**
     * Create failure outcome.
     *
     * @param failure the value
     * @param <A>     the value type
     * @return the failure outcome
     */
    public static <A> Outcome<A> failure(final Throwable failure) {
        return new Failure<>(failure);
    }

    /**
     * Create outcome basing on BiConsumer arguments.
     *
     * @param value   the value
     * @param failure the failure
     * @param <A>     the value type
     * @return the outcome
     */
    public static <A> Outcome<A> of(final A value, final Throwable failure) {
        if (failure != null) {
            return failure(failure);
        } else {
            return success(value);
        }
    }

    /**
     * Notify listener.
     *
     * @param listener the listener
     * @param outcome  the outcome
     * @param <T>      the outcome
     */
    @SuppressWarnings("unchecked")
    public static <T> void notifyResolver(final AResolver<? super T> listener, final Outcome<T> outcome) {
        try {
            if (outcome == null) {
                listener.resolve(Outcome.failure(new IllegalArgumentException("Outcome must not be null")));
            } else {
                listener.resolve((Outcome) outcome);
            }
        } catch (Throwable ex) {
            LOG.error("Failed to notify listener", ex);
        }
    }

    /**
     * Notify listener.
     *
     * @param listener the listener
     * @param value    the outcome
     * @param <T>      the outcome
     */
    public static <T> void notifySuccess(final AResolver<T> listener, final T value) {
        notifyResolver(listener, Outcome.success(value));
    }

    /**
     * Notify listener.
     *
     * @param <T>      the outcome
     * @param listener the listener
     * @param problem  the outcome failure
     */
    public static <T> void notifyFailure(final AResolver<T> listener, final Throwable problem) {
        notifyResolver(listener, Outcome.failure(problem));
    }

    /**
     * Force the value to appear.
     *
     * @return the value if it is success outcome
     * @throws Throwable if it is a failure outcome
     */
    @SuppressWarnings("squid:S00112")
    public abstract T force() throws Throwable;

    /**
     * Get value for the outcome if it is a success outcome.
     *
     * @return the current value
     * @throws IllegalStateException if no value is available
     */
    public abstract T value();

    /**
     * Get value for the outcome if it is a failure outcome.
     *
     * @return the failure
     * @throws IllegalStateException if this outcome is a failure outcome
     */
    public abstract Throwable failure();

    /**
     * @return true if this is a success outcome
     */
    public abstract boolean isSuccess();

    /**
     * @return true if this is a failure outcome
     */
    public abstract boolean isFailure();
}
