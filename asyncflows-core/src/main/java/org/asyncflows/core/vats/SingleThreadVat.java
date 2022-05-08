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

package org.asyncflows.core.vats;

import java.util.concurrent.Semaphore;

/**
 * The single thread vat.
 */
public final class SingleThreadVat extends SingleThreadVatWithIdle {
    /**
     * The semaphore.
     */
    private final Semaphore semaphore = new Semaphore(1);

    /**
     * The constructor with the specified batch size.
     *
     * @param stopKey the key used to stop the vat
     */
    public SingleThreadVat(final Object stopKey) {
        super(Integer.MAX_VALUE, stopKey);
    }

    @Override
    protected void idle() {
        semaphore.acquireUninterruptibly();
    }

    @Override
    protected void pollIdle() {
        // do nothing
    }

    @Override
    protected void closeVat() {
        wakeUp();
    }

    @Override
    protected void wakeUp() {
        if (semaphore.availablePermits() < 1) {
            semaphore.release();
        }
    }
}
