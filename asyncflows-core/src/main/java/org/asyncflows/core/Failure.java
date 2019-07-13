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

import java.util.Objects;

/**
 * The failure outcome.
 *
 * @param <T> the value type
 */
public final class Failure<T> extends Outcome<T> {
    /**
     * The failure outcome.
     */
    private final Throwable failure;

    /**
     * The constructor from failure.
     *
     * @param failure the failure
     */
    public Failure(final Throwable failure) {
        this.failure = failure != null ? failure : new IllegalArgumentException("failure is null");
    }

    /**
     * Convert the failure to other type. The failure could be converted to any type, since its
     * content does not depends on the value type of outcome.
     *
     * @param <A> new type of the failure
     * @return the same failure by with changed type
     */
    @SuppressWarnings("unchecked")
    public <A> Failure<A> toOtherType() {
        return (Failure<A>) (Failure) this;
    }

    @Override
    public T force() throws Throwable {
        throw failure;
    }

    @Override
    public T value() {
        throw new IllegalStateException("This is a failure outcome", failure);
    }

    @Override
    public Throwable failure() {
        return failure;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isFailure() {
        return true;
    }

    // NOPMD
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Failure failure1 = (Failure) o;

        return Objects.equals(failure, failure1.failure);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(failure);
    }

    @Override
    public String toString() {
        return "Failure{" + failure + '}';
    }
}
