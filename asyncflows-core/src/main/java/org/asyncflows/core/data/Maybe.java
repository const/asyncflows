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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Maybe<?> maybe = (Maybe<?>) o;
        return valuePresent == maybe.valuePresent &&
                Objects.equals(value, maybe.value);
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