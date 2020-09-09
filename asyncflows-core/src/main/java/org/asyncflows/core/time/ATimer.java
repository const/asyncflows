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

package org.asyncflows.core.time;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.streams.AStream;

import java.time.Duration;
import java.time.Instant;

/**
 * The timer interface.
 */
@Asynchronous
public interface ATimer extends ACloseable {
    /**
     * Sleep for the specified duration.
     *
     * @param duration the duration
     * @return when sleep finishes
     */
    default Promise<Instant> sleep(Duration duration) {
        return waitFor(Instant.now().plus(duration));
    }

    /**
     * Wait until the specified time.
     *
     * @param time the time to wait for
     * @return promise for the time when sleep was scheduled to awake
     */
    Promise<Instant> waitFor(Instant time);

    /**
     * Start fixed rate stream timer stream.
     *
     * @param firstTime the the first time the value is returned from the stream
     * @param period    the period with which events happen
     * @return promise for stream, note the stream accumulate events if they were not asked for
     */
    Promise<AStream<Instant>> fixedRate(Instant firstTime, Duration period);

    /**
     * Start fixed rate stream timer stream.
     *
     * @param initialDelay the delay before first event
     * @param period       the period with which events happen
     * @return promise for stream, note the stream accumulate events if they were not asked for
     */
    default Promise<AStream<Instant>> fixedRate(Duration initialDelay, Duration period) {
        return fixedRate(Instant.now().plus(initialDelay), period);
    }

    /**
     * Start fixed rate stream timer stream.
     *
     * @param period the period with which events happen
     * @return promise for stream, note the stream accumulate events if they were not asked for
     */
    default Promise<AStream<Instant>> fixedRate(Duration period) {
        return fixedRate(Instant.now(), period);
    }

    /**
     * Start delay rate stream timer stream. The each reading from stream is delayed
     * for the specified time. It is assumed that caller would ask for the next value,
     * when it finished task for the first one.
     *
     * @param firstTime the the first time the value is returned from the stream
     * @param delay     the delay between events
     * @return promise for stream
     */
    Promise<AStream<Instant>> fixedDelay(Instant firstTime, Duration delay);

    /**
     * Start delay rate stream timer stream. The each reading from stream is delayed
     * for the specified time. It is assumed that caller would ask for the next value,
     * when it finished task for the previous one.
     *
     * @param initialDelay the delay before first event
     * @param delay        the delay between events
     * @return promise for stream
     */
    default Promise<AStream<Instant>> fixedDelay(Duration initialDelay, Duration delay) {
        return fixedDelay(Instant.now().plus(initialDelay), delay);
    }

    /**
     * Start delay rate stream timer stream. The each reading from stream is delayed
     * for the specified time. It is assumed that caller would ask for the next value,
     * when it finished task for the previous one.
     *
     * @param delay the delay between events
     * @return promise for stream
     */
    default Promise<AStream<Instant>> fixedDelay(Duration delay) {
        return fixedDelay(Instant.now(), delay);
    }
}
