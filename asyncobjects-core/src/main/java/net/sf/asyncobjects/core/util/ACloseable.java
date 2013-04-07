package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.Promise;

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
