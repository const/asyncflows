package org.asyncflows.core.streams.time;

import org.asyncflows.core.data.Tuple3;
import org.asyncflows.core.streams.StreamUtil;
import org.asyncflows.core.time.Timer;
import org.asyncflows.core.util.ResourceUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.streams.AsyncStreams.aForStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test for the timer functionality.
 */
public class TimerTest {

    @Test
    public void sleepTest() throws Throwable {
        final Tuple3<Long, Long, Long> r = doAsync(() ->
                ResourceUtil.aTryResource(new Timer()).run(timer -> {
                    final long start = System.nanoTime();
                    return timer.sleep(20).flatMap(value -> {
                        long end = System.nanoTime();
                        return aValue(Tuple3.of(start, end, value));
                    });
                }));
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    public void waitForTest() throws Throwable {
        final Tuple3<Long, Long, Long> r = doAsync(() ->
                ResourceUtil.aTryResource(new Timer()).run(timer -> {
                    final long start = System.nanoTime();
                    return timer.waitFor(new Date(System.currentTimeMillis() + 100)).flatMap(
                            value -> {
                                long end = System.nanoTime();
                                return aValue(Tuple3.of(start, end, value));
                            });
                }));
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    public void fixedRate() throws Throwable {
        final long start = System.nanoTime();
        final List<Long> r = doAsync(() ->
                ResourceUtil.aTryResource(new Timer()).run(timer ->
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
    public void fixedDelay() throws Throwable {
        final long start = System.nanoTime();
        final List<Long> r = doAsync(() -> ResourceUtil.aTryResource(new Timer()).run(
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
