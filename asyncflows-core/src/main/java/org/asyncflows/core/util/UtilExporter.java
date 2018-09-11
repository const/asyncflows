/*
 * Copyright (c) 2018 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.core.util;

import org.asyncflows.core.Promise;
import org.asyncflows.core.vats.Vat;

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
    static ASemaphore export(final Vat vat, final ASemaphore semaphore) {
        // TODO refactor all exports use actually inner classes with inheritance
        return new ASemaphore() {
            @Override
            public void release(final int permits) {
                aSend(vat, () -> semaphore.release(permits));
            }

            @Override
            public void release() {
                aSend(vat, semaphore::release);
            }

            @Override
            public Promise<Void> acquire() {
                return aLater(vat, semaphore::acquire);
            }

            @Override
            public Promise<Void> acquire(final int permits) {
                return aLater(vat, () -> semaphore.acquire(permits));
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
    static <T> AQueue<T> export(final Vat vat, final AQueue<T> queue) {
        return new AQueue<T>() {
            @Override
            public Promise<T> take() {
                return aLater(vat, queue::take);
            }

            @Override
            public Promise<Void> put(final T element) {
                return aLater(vat, () -> queue.put(element));
            }
        };
    }

}
