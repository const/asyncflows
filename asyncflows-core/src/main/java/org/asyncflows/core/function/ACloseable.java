package org.asyncflows.core.function;

import org.asyncflows.core.Promise;

/**
 * Closeable object.
 */
public interface ACloseable {
    /**
     * The close operation.
     *
     * @return promise that finishes when close operation is complete
     */
    Promise<Void> close();
}
