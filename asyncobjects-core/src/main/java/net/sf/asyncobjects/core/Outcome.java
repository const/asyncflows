package net.sf.asyncobjects.core;

/**
 * The outcome type.
 *
 * @param <T> the value type
 */
public abstract class Outcome<T> {
    /**
     * Force the value to appear.
     *
     * @return the value if it is success outcome
     * @throws Throwable if it is a failure outcome
     */
    public abstract T force() throws Throwable;

    /**
     * Get value for the outcome if it is a success outcome.
     *
     * @return the current value
     * @throws IllegalStateException if no value is available
     */
    public abstract T value();

    /**
     * Get value for the outcome if it is a failure outcome.
     *
     * @return the failure
     * @throws IllegalStateException if this outcome is a failure outcome
     */
    public abstract Throwable failure();

    /**
     * @return true if this is a success outcome
     */
    public abstract boolean isSuccess();

    /**
     * Upcast outcome value to a weaker type.
     *
     * @param input the input outcome
     * @param <O>   the output type
     * @param <I>   the input type
     * @return Upcasted the outcome
     */
    @SuppressWarnings("unchecked")
    public static <O, I extends O> Outcome<O> upcast(final Outcome<I> input) {
        return (Outcome<O>) input;
    }

    /**
     * Create success outcome.
     *
     * @param value the value
     * @param <A>   the value type
     * @return the success outcome
     */
    public static <A> Outcome<A> success(final A value) {
        return new Success<A>(value);
    }

    /**
     * Create failure outcome.
     *
     * @param failure the value
     * @param <A>     the value type
     * @return the failure outcome
     */
    public static <A> Outcome<A> failure(final Throwable failure) {
        return new Failure<A>(failure);
    }

}
