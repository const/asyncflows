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

package org.asyncflows.protocol.http.common.content;

import org.asyncflows.core.Outcome;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The base for the counting stream.
 */
public class CountingStreamBase {
    /**
     * The time when observing started.
     */
    private final long started = System.currentTimeMillis();
    /**
     * The listener for the event.
     */
    private final Consumer<StreamFinishedEvent> listener;
    /**
     * True if event fired.
     */
    private final AtomicBoolean firedEvent = new AtomicBoolean(false);
    /**
     * Amount of bytes transferred.
     */
    private final AtomicLong transferred = new AtomicLong(0L);

    /**
     * The constructor.
     *
     * @param listener the listener (must be exported, or be otherwise safe for use from other threads)
     */
    public CountingStreamBase(final Consumer<StreamFinishedEvent> listener) {
        this.listener = listener;
    }

    /**
     * Count transferred bytes.
     *
     * @param count the transferred amount.
     */
    protected final void transferred(final int count) {
        transferred.addAndGet(count);
    }

    /**
     * Fire event.
     *
     * @param failure the failure (null if success)
     */
    protected final void fireFinished(final Throwable failure) {
        if (firedEvent.compareAndSet(false, true)) {
            listener.accept(new StreamFinishedEvent(started, System.currentTimeMillis(), transferred.get(), failure));
        }
    }

    /**
     * Fire event.
     */
    protected final void fireFinished() {
        fireFinished((Throwable) null);
    }

    /**
     * Fire event.
     *
     * @param resolution the failure source
     */
    protected final void fireFinished(final Outcome<?> resolution) {
        //noinspection ThrowableResultOfMethodCallIgnored
        fireFinished(resolution.isSuccess() ? null : resolution.failure());
    }

}
