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

package org.asyncflows.core.streams.time;

import org.asyncflows.core.data.Tuple3;
import org.asyncflows.core.streams.StreamUtil;
import org.asyncflows.core.time.Timer;
import org.asyncflows.core.util.CoreFlowsResource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.streams.AsyncStreams.aForStream;
import static org.asyncflows.core.util.CancellableFlows.aWithLocalCancellation;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test for the timer functionality.
 */
class TimerTest {

    @Test
    void sleepTest() {
        final Tuple3<Long, Long, Instant> r = doAsync(() ->
                CoreFlowsResource.aTryResource(new Timer()).run(timer -> {
                    final long start = System.nanoTime();
                    return timer.sleep(Duration.ofMillis(20)).flatMap(value -> {
                        long end = System.nanoTime();
                        return aValue(Tuple3.of(start, end, value));
                    });
                }));
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    void sleepCancelTest() {
        doAsync(() -> aWithLocalCancellation(c -> CoreFlowsResource.aTryResource(new Timer()).run(timer -> aAll(() ->
                timer.sleep(Duration.ofMillis(10)).listen(o -> c.cancel())
        ).andLast(() -> timer.sleep(Duration.ofHours(1)).flatMapOutcome(o -> {
                    assertTrue(o.isFailure());
                    assertEquals(CancellationException.class, o.failure().getClass());
                    return aVoid();
                })
        ))));
    }

    @Test
    void waitForTest() {
        final Tuple3<Long, Long, Instant> r = doAsync(() ->
                CoreFlowsResource.aTryResource(new Timer()).run(timer -> {
                    final long start = System.nanoTime();
                    return timer.waitFor(Instant.now().plusMillis(100)).flatMap(
                            value -> {
                                long end = System.nanoTime();
                                return aValue(Tuple3.of(start, end, value));
                            });
                }));
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    void fixedRate() {
        final long start = System.nanoTime();
        final List<Instant> r = doAsync(() ->
                CoreFlowsResource.aTryResource(new Timer()).run(timer ->
                        timer.fixedRate(Duration.ofMillis(5), Duration.ofMillis(5)).flatMap(value -> aForStream(StreamUtil.head(value, 5)).toList())));
        final long end = System.nanoTime();
        assertTrue(start + TimeUnit.MILLISECONDS.toNanos(25) <= end);
        assertEquals(5, r.size());
        final Iterator<Instant> i = r.iterator();
        long t = i.next().toEpochMilli();
        while (i.hasNext()) {
            final long t2 = i.next().toEpochMilli();
            assertEquals(t + 5, t2);
            t = t2;
        }
    }

    @Test
    void fixedDelay() {
        final long start = System.nanoTime();
        final List<Instant> r = doAsync(() -> CoreFlowsResource.aTryResource(new Timer()).run(
                timer -> timer.fixedDelay(Duration.ofMillis(5), Duration.ofMillis(5)).flatMap(value -> aForStream(StreamUtil.head(value, 5)).toList())));
        final long end = System.nanoTime();
        assertTrue(start + TimeUnit.MILLISECONDS.toNanos(25) <= end);
        assertEquals(5, r.size());
        final Iterator<Instant> i = r.iterator();
        long t = i.next().toEpochMilli();
        while (i.hasNext()) {
            final long t2 = i.next().toEpochMilli();
            assertTrue(t + 5 <= t2);
            t = t2;
        }
    }
}
