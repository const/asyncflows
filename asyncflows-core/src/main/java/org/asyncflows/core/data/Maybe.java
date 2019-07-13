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

package org.asyncflows.core.data;

import java.util.Objects;

/**
 * The optional value type. It is intentionally named differently from the JDK8 Optional and Scala Option types.
 *
 * @param <T> the value type
 */
public final class Maybe<T> {
    /**
     * The empty value.
     */
    private static final Maybe<?> EMPTY_VALUE = new Maybe<>(false, null);
    /**
     * If true there is a value.
     */
    private final boolean valuePresent;
    /**
     * The value (in case when {@link #valuePresent} == true).
     */
    private final T value;

    /**
     * The constructor.
     *
     * @param valuePresent if true, there is a value
     * @param value        the value
     */
    private Maybe(final boolean valuePresent, final T value) {
        this.valuePresent = valuePresent;
        this.value = value;
    }

    /**
     * Create empty value.
     *
     * @param <A> the value type
     * @return the empty value
     */
    @SuppressWarnings("unchecked")
    public static <A> Maybe<A> empty() {
        return (Maybe<A>) EMPTY_VALUE;
    }

    /**
     * The constructor for value.
     *
     * @param value the value
     * @param <A>   the value type
     * @return the option value
     */
    public static <A> Maybe<A> value(final A value) {
        return new Maybe<>(true, value);
    }

    /**
     * @return true if option has a value
     */
    public boolean hasValue() {
        return valuePresent;
    }

    /**
     * @return true if value is empty
     */
    public boolean isEmpty() {
        return !valuePresent;
    }

    /**
     * @return the value
     */
    public T value() {
        if (!valuePresent) {
            throw new IllegalStateException("No value in this optional value");
        }
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Maybe<?> maybe = (Maybe<?>) o;
        return valuePresent == maybe.valuePresent && Objects.equals(value, maybe.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valuePresent, value);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Maybe{");
        if (valuePresent) {
            sb.append(value);
        }
        sb.append('}');
        return sb.toString();
    }
}
