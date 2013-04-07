package net.sf.asyncobjects.core;

/**
 * The mapper interface.
 *
 * @param <R> the output type
 * @param <A> the input type
 */
public interface AFunction<R, A> {
    /**
     * Map the value.
     *
     * @param value the value to map
     * @return the output type
     * @throws Throwable in case of any problem
     */
    Promise<R> apply(A value) throws Throwable;
}
