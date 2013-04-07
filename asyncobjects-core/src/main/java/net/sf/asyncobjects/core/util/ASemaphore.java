package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.Promise;

/**
 * Semaphore.
 */
public interface ASemaphore {
    /**
     * Release permits.
     *
     * @param permits the permits to release
     */
    void release(int permits);

    /**
     * Release one permit.
     */
    void release();

    /**
     * @return acquire one permit
     */
    Promise<Void> acquire();

    /**
     * Acquire many permits.
     *
     * @param permits the permits to acquire
     * @return acquire one permit
     */
    Promise<Void> acquire(int permits);
}
