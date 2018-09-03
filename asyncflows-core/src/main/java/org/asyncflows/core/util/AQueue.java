package org.asyncflows.core.util;

import org.asyncflows.core.Promise;

/**
 * AQueue of elements.
 *
 * @param <T> the element type
 */
public interface AQueue<T> {
    /**
     * Get element from the queue.
     *
     * @return the promise that resolves when element is available
     */
    Promise<T> take();

    /**
     * Put element to the queue.
     *
     * @param element the element to put
     * @return the promise that resolves when queue is ready for the next element.
     */
    Promise<Void> put(T element);
}
