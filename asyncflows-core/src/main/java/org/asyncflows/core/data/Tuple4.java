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
 * The tuple of three values.
 *
 * @param <T1> the type of the fist value
 * @param <T2> the type of the second value
 * @param <T3> the type of the third value
 * @param <T4> the type of the forth value
 */
public final class Tuple4<T1, T2, T3, T4> {
    /**
     * The 1st value.
     */
    private final T1 value1;
    /**
     * The 2nd value.
     */
    private final T2 value2;
    /**
     * The 3rd value.
     */
    private final T3 value3;

    /**
     * The 4th value.
     */
    private final T4 value4;

    /**
     * The three element tuple.
     *
     * @param value1 the 1st value
     * @param value2 the 2nd value
     * @param value3 the 3rd value
     * @param value4 the 4th value
     */
    public Tuple4(final T1 value1, final T2 value2, final T3 value3, final T4 value4) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    /**
     * Make tuple.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param value4 the forth value
     * @param <T1>   the first value type
     * @param <T2>   the second value type
     * @param <T3>   the third value type
     * @param <T4>   the type of the forth value
     * @return the tuple
     */
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(final T1 value1, final T2 value2, final T3 value3,
                                                             final T4 value4) {
        return new Tuple4<>(value1, value2, value3, value4);
    }

    /**
     * @return the 1st value
     */
    public T2 getValue2() {
        return value2;
    }

    /**
     * @return the 2nd value
     */
    public T1 getValue1() {
        return value1;
    }

    /**
     * @return the 3rd value
     */
    public T3 getValue3() {
        return value3;
    }

    /**
     * @return the 3rd value
     */
    public T4 getValue4() {
        return value4;
    }

    /**
     * Map tuple.
     *
     * @param body body
     * @param <R>  the result type
     * @return the result
     */
    public <R> R map(Function4<T1, T2, T3, T4, R> body) {
        return body.apply(getValue1(), getValue2(), getValue3(), getValue4());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;
        return Objects.equals(value1, tuple4.value1)
                && Objects.equals(value2, tuple4.value2)
                && Objects.equals(value3, tuple4.value3)
                && Objects.equals(value4, tuple4.value4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value1, value2, value3, value4);
    }

    @Override
    public String toString() {
        return "(" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + ')';
    }
}
