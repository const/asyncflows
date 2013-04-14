package net.sf.asyncobjects.core.data;

/**
 * The optional value type. It is intentionally named differently from the JDK8 Optional and Scala Option types.
 *
 * @param <T> the value type
 */
public final class Maybe<T> {
    /**
     * The empty value.
     */
    private static final Maybe<?> EMPTY_VALUE = new Maybe<Object>(false, null);
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
        return new Maybe<A>(true, value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Maybe that = (Maybe) o;

        if (valuePresent != that.valuePresent) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = valuePresent ? 1 : 0;
        final int prime = 31;
        result = prime * result + (value != null ? value.hashCode() : 0);
        return result;
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
