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

package org.asyncflows.core.streams.time;

import org.asyncflows.core.data.Tuple3;
import org.asyncflows.core.streams.StreamUtil;
import org.asyncflows.core.time.Timer;
import org.asyncflows.core.util.CoreFlowsResource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.streams.AsyncStreams.aForStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test for the timer functionality.
 */
public class TimerTest {

    @Test
    public void sleepTest() {
        final Tuple3<Long, Long, Long> r = doAsync(() ->
                CoreFlowsResource.aTryResource(new Timer()).run(timer -> {
                    final long start = System.nanoTime();
                    return timer.sleep(20).flatMap(value -> {
                        long end = System.nanoTime();
                        return aValue(Tuple3.of(start, end, value));
                    });
                }));
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    public void waitForTest() {
        final Tuple3<Long, Long, Long> r = doAsync(() ->
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
    public void fixedRate() {
        final long start = System.nanoTime();
        final List<Long> r = doAsync(() ->
                CoreFlowsResource.aTryResource(new Timer()).run(timer ->
                        timer.fixedRate(5, 5).flatMap(value -> aForStream(StreamUtil.head(value, 5)).toList())));
        final long end = System.nanoTime();
        assertTrue(start + TimeUnit.MILLISECONDS.toNanos(25) <= end);
        assertEquals(5, r.size());
        final Iterator<Long> i = r.iterator();
        long t = i.next();
        while (i.hasNext()) {
            final long t2 = i.next();
            assertEquals(t + 5, t2);
            t = t2;
        }
    }

    @Test
    public void fixedDelay() {
        final long start = System.nanoTime();
        final List<Long> r = doAsync(() -> CoreFlowsResource.aTryResource(new Timer()).run(
                timer -> timer.fixedDelay(5, 5).flatMap(value -> aForStream(StreamUtil.head(value, 5)).toList())));
        final long end = System.nanoTime();
        assertTrue(start + TimeUnit.MILLISECONDS.toNanos(25) <= end);
        assertEquals(5, r.size());
        final Iterator<Long> i = r.iterator();
        long t = i.next();
        while (i.hasNext()) {
            final long t2 = i.next();
            assertTrue(t + 5 <= t2);
            t = t2;
        }
    }
}
