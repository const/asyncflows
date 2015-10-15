package net.sf.asyncobjects.core;

/**
 * An asynchronous action that returns result.
 *
 * @param <T> the result type
 */
@FunctionalInterface
public interface ACallable<T> {
    /**
     * Run action that expects unit promise.
     *
     * @return promise for result
     * @throws Exception in case of any problem
     */
    Promise<T> call() throws Exception;
}
