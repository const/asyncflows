package net.sf.asyncobjects.core;

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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Failure failure1 = (Failure) o;

        //noinspection RedundantIfStatement
        if (!failure.equals(failure1.failure)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return failure.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Failure{");
        sb.append(failure);
        sb.append('}');
        return sb.toString();
    }
}
