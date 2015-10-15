package net.sf.asyncobjects.core;

/**
 * The success outcome.
 *
 * @param <T> the value type
 */
public final class Success<T> extends Outcome<T> {
    /**
     * The value.
     */
    private final T value;

    /**
     * Create a success outcome.
     *
     * @param value the value
     */
    public Success(final T value) {
        this.value = value;
    }

    @Override
    public T force() throws Throwable {
        return value;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public Throwable failure() {
        throw new IllegalStateException("No failure is available");
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean isFailure() {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Success success = (Success) o;

        //noinspection RedundantIfStatement
        if (value != null ? !value.equals(success.value) : success.value != null) { // NOPMD
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Success{" + value + '}';
    }
}
