package org.asyncflows.core.data;

import java.util.Objects;

/**
 * The tuple of two values.
 *
 * @param <T1> the type of the fist value
 * @param <T2> the type of the second value
 */
public final class Tuple2<T1, T2> {
    /**
     * The 1st value.
     */
    private final T1 value1;
    /**
     * The 2nd value.
     */
    private final T2 value2;

    /**
     * The two element tuple.
     *
     * @param value1 the 1st value
     * @param value2 the 2nd value
     */
    public Tuple2(final T1 value1, final T2 value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    /**
     * Make tuple.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @param <T1>   the first value type
     * @param <T2>   the second value type
     * @return the tuple
     */
    public static <T1, T2> Tuple2<T1, T2> of(final T1 value1, final T2 value2) {
        return new Tuple2<>(value1, value2);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equals(value1, tuple2.value1) &&
                Objects.equals(value2, tuple2.value2);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value1, value2);
    }

    @Override
    public String toString() {
        return "(" + value1 + ", " + value2 + ')';
    }
}
