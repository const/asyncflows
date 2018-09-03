package org.asyncflows.core.util;

import org.asyncflows.core.Promise;

import java.util.concurrent.Executor;

import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aSend;

/**
 * Exports for utility classes.
 */
final class UtilExporter {
    /**
     * A private constructor for utility class.
     */
    private UtilExporter() {
    }

    /**
     * Export for semaphore.
     *
     * @param vat       the vat
     * @param semaphore the semaphore
     * @return the exported class
     */
    static ASemaphore export(final Executor vat, final ASemaphore semaphore) {
        // TODO refactor all exports use actually inner classes with inheritance
        return new ASemaphore() {
            @Override
            public void release(final int permits) {
                aSend(() -> semaphore.release(permits), vat);
            }

            @Override
            public void release() {
                aSend(semaphore::release, vat);
            }

            @Override
            public Promise<Void> acquire() {
                return aLater(semaphore::acquire, vat);
            }

            @Override
            public Promise<Void> acquire(final int permits) {
                return aLater(() -> semaphore.acquire(permits), vat);
            }
        };
    }

    /**
     * The export for queue.
     *
     * @param vat   the vat
     * @param queue the queue
     * @param <T>   the element type
     * @return the exported queue
     */
    static <T> AQueue<T> export(final Executor vat, final AQueue<T> queue) {
        return new AQueue<T>() {
            @Override
            public Promise<T> take() {
                return aLater(queue::take, vat);
            }

            @Override
            public Promise<Void> put(final T element) {
                return aLater(() -> queue.put(element), vat);
            }
        };
    }

}
