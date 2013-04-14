package net.sf.asyncobjects.core;

/**
 * An asynchronous action that returns result.
 *
 * @param <T> the result type
 */
public interface ACallable<T> {
    /**
     * Run action that expects unit promise.
     *
     * @return promise for result
     * @throws Throwable in case of any problem
     */
    Promise<T> call() throws Throwable;
}
