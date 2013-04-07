package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.Promise;

/**
 * The two argument function.
 *
 * @param <R> the result type
 * @param <A> the first argument type
 * @param <B> the second argument type
 */
public interface AFunction2<R, A, B> {
    /**
     * The apply operation.
     *
     * @param value1 the first argument
     * @param value2 the second argument
     * @return the result
     * @throws Throwable in case of any problem
     */
    Promise<R> apply(A value1, B value2) throws Throwable;
}
