package net.sf.asyncobjects.core.data;

/**
 * The tuple of three values.
 *
 * @param <T1> the type of the fist value
 * @param <T2> the type of the second value
 * @param <T3> the type of the third value
 */
public final class Tuple3<T1, T2, T3> {
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
     * The three element tuple.
     *
     * @param value1 the 1st value
     * @param value2 the 2nd value
     * @param value3 the 3rd value
     */
    public Tuple3(final T1 value1, final T2 value2, final T3 value3) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Tuple3 tuple3 = (Tuple3) o;

        if (value1 != null ? !value1.equals(tuple3.value1) : tuple3.value1 != null) {
            return false;
        }
        if (value2 != null ? !value2.equals(tuple3.value2) : tuple3.value2 != null) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (value3 != null ? !value3.equals(tuple3.value3) : tuple3.value3 != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = value1 != null ? value1.hashCode() : 0;
        final int prime = 31;
        result = prime * result + (value2 != null ? value2.hashCode() : 0);
        result = prime * result + (value3 != null ? value3.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("(");
        sb.append(value1);
        sb.append(", ").append(value2);
        sb.append(", ").append(value3);
        sb.append(')');
        return sb.toString();
    }

    /**
     * Make tuple.
     *
     * @param value1 the first value
     * @param value2 the second value
     * @param value3 the third value
     * @param <T1>   the first value type
     * @param <T2>   the second value type
     * @param <T3>   the third value type
     * @return the tuple
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(final T1 value1, final T2 value2, final T3 value3) {
        return new Tuple3<T1, T2, T3>(value1, value2, value3);
    }
}
