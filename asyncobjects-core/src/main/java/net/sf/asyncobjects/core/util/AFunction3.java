package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.Promise;

/**
 * The three argument function.
 *
 * @param <R> the result type
 * @param <A> the first argument type
 * @param <B> the second argument type
 * @param <C> the third argument type
 */
@FunctionalInterface
public interface AFunction3<R, A, B, C> {
    /**
     * The apply operation.
     *
     * @param value1 the first argument
     * @param value2 the second argument
     * @param value3 the third argument
     * @return the result
     * @throws Exception in case of any problem
     */
    Promise<R> apply(A value1, B value2, C value3) throws Exception;
}
