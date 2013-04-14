package net.sf.asyncobjects.core.data;

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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Tuple2 tuple2 = (Tuple2) o;

        if (value1 != null ? !value1.equals(tuple2.value1) : tuple2.value1 != null) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (value2 != null ? !value2.equals(tuple2.value2) : tuple2.value2 != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = value1 != null ? value1.hashCode() : 0;
        final int prime = 31;
        result = prime * result + (value2 != null ? value2.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("(");
        sb.append(value1);
        sb.append(", ").append(value2);
        sb.append(')');
        return sb.toString();
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
        return new Tuple2<T1, T2>(value1, value2);
    }
}
