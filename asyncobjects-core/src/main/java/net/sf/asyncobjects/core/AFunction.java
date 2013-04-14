package net.sf.asyncobjects.core;

/**
 * The function interface.
 *
 * @param <R> the output type
 * @param <A> the input type
 */
public interface AFunction<R, A> {
    /**
     * Evaluate result.
     *
     * @param value the value to map
     * @return the output type
     * @throws Throwable in case of any problem
     */
    Promise<R> apply(A value) throws Throwable;
}
