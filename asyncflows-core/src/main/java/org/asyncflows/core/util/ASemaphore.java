/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
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
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.ASupplier;

/**
 * Semaphore.
 */
@Asynchronous
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

    /**
     * Execute action guarded by semaphore. The action is executed in the client context.
     *
     * @param action the action
     * @param <T>    the result type
     * @return the promise that resolves when action finishes to execute.
     */
    default <T> Promise<T> run(ASupplier<T> action) {
        return acquire().thenFlatGet(action).listenSync(o -> release());
    }
}
