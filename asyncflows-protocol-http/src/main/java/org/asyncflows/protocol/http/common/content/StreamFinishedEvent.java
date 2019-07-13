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

/**
 * The event that is used to notify that work with stream has been finished.
 *
 * @see CountingInput
 * @see CountingOutput
 */
public class StreamFinishedEvent {
    /**
     * Stream measurements started time.
     */
    private final long started;
    /**
     * Stream measurements finished time.
     */
    private final long finished;
    /**
     * Amount of bytes transferred.
     */
    private final long transferred;
    /**
     * The failure returned from read operation.
     */
    private final Throwable failure;

    /**
     * The constructor.
     *
     * @param started     the start time (ms)
     * @param finished    the finished time (ms)
     * @param transferred the amount transferred
     * @param failure     the failure
     */
    public StreamFinishedEvent(final long started, final long finished,
                               final long transferred, final Throwable failure) {
        this.started = started;
        this.finished = finished;
        this.transferred = transferred;
        this.failure = failure;
    }

    /**
     * @return the start time
     */
    public long getStarted() {
        return started;
    }

    /**
     * @return the finish time
     */
    public long getFinished() {
        return finished;
    }

    /**
     * @return the amount of bytes transferred
     */
    public long getTransferred() {
        return transferred;
    }

    /**
     * @return the failure or null.
     */
    public Throwable getFailure() {
        return failure;
    }

    @Override
    public String toString() {
        return "StreamFinishedEvent{ started=" + started + ", duration=" + (finished - started)
                + ", transferred=" + transferred + (failure == null ? "" : ", failure=" + failure) + '}';
    }
}
