/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

package org.asyncflows.core.vats;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The single thread vat with an idle action.
 */
public abstract class SingleThreadVatWithIdle extends BatchedVat {
    /**
     * The stop object.
     */
    protected final Object myStopKey;
    /**
     * If true, the vat is stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * The constructor.
     *
     * @param maxBatchSize max batch size
     * @param stopKey      the stop key
     */
    public SingleThreadVatWithIdle(final int maxBatchSize, final Object stopKey) {
        super(maxBatchSize);
        myStopKey = stopKey;
    }

    /**
     * Start the vat in the current thread.
     */
    public void runInCurrentThread() {
        boolean hasMore = true;
        while (!stopped.get()) {
            if (hasMore) {
                pollIdle();
            } else {
                idle();
            }
            hasMore = runBatch();
        }
    }

    /**
     * The idle action.
     */
    protected abstract void idle();

    /**
     * The wake up action.
     */
    protected abstract void wakeUp();

    /**
     * Close the vat, wake up if needed.
     */
    protected abstract void closeVat();

    /**
     * Poll idle.
     */
    protected abstract void pollIdle();

    /**
     * Stop the vat.
     *
     * @param stopKey the key used to stop the vat
     */
    public void stop(final Object stopKey) {
        if (myStopKey != stopKey) { // NOPMD
            throw new IllegalArgumentException("The stop key is invalid");
        }
        if (stopped.compareAndSet(false, true)) {
            closeVat();
        }
    }

    @Override
    protected void schedule() {
        if (stopped.get()) {
            throw new IllegalStateException("The vat is already stopped");
        }
        wakeUp();
    }
}
