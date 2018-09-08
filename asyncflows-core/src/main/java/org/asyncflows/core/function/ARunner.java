package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

/**
 * A runner for actions.
 */
public interface ARunner {
    /**
     * Start asynchronous action somewhere.
     *
     * @param action an action
     * @param <T>    the type of action
     * @return the promise for action
     */
    <T> Promise<T> run(ASupplier<T> action);
}
