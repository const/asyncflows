package net.sf.asyncobjects.core.streams.time;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.core.stream.AStream;
import net.sf.asyncobjects.core.stream.StreamUtil;
import net.sf.asyncobjects.core.time.Timer;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsyncThrowable;
import static net.sf.asyncobjects.core.stream.Streams.aForStream;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The test for the timer functionality.
 */
public class TimerTest {

    @Test
    public void sleepTest() throws Throwable {
        final Tuple3<Long, Long, Long> r = doAsyncThrowable(new ACallable<Tuple3<Long, Long, Long>>() {
            @Override
            public Promise<Tuple3<Long, Long, Long>> call() throws Throwable {
                return aTry(new Timer()).run(new AFunction<Tuple3<Long, Long, Long>, Timer>() {
                    @Override
                    public Promise<Tuple3<Long, Long, Long>> apply(final Timer timer) throws Throwable {
                        final long start = System.nanoTime();
                        return timer.sleep(10).map(new AFunction<Tuple3<Long, Long, Long>, Long>() {
                            @Override
                            public Promise<Tuple3<Long, Long, Long>> apply(final Long value) throws Throwable {
                                long end = System.nanoTime();
                                return aValue(Tuple3.of(start, end, value));
                            }
                        });
                    }
                });
            }
        });
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    public void waitForTest() throws Throwable {
        final Tuple3<Long, Long, Long> r = doAsyncThrowable(new ACallable<Tuple3<Long, Long, Long>>() {
            @Override
            public Promise<Tuple3<Long, Long, Long>> call() throws Throwable {
                return aTry(new Timer()).run(new AFunction<Tuple3<Long, Long, Long>, Timer>() {
                    @Override
                    public Promise<Tuple3<Long, Long, Long>> apply(final Timer timer) throws Throwable {
                        final long start = System.nanoTime();
                        return timer.waitFor(new Date(System.currentTimeMillis() + 10)).map(
                                new AFunction<Tuple3<Long, Long, Long>, Long>() {
                                    @Override
                                    public Promise<Tuple3<Long, Long, Long>> apply(final Long value) throws Throwable {
                                        long end = System.nanoTime();
                                        return aValue(Tuple3.of(start, end, value));
                                    }
                                });
                    }
                });
            }
        });
        assertTrue(r.getValue1() + TimeUnit.MILLISECONDS.toNanos(10) <= r.getValue2());
    }

    @Test
    public void fixedRate() throws Throwable {
        final long start = System.nanoTime();
        final List<Long> r = doAsyncThrowable(new ACallable<List<Long>>() {
            @Override
            public Promise<List<Long>> call() throws Throwable {
                return aTry(new Timer()).run(new AFunction<List<Long>, Timer>() {
                    @Override
                    public Promise<List<Long>> apply(final Timer timer) throws Throwable {
                        return timer.fixedRate(5, 5).map(new AFunction<List<Long>, AStream<Long>>() {
                            @Override
                            public Promise<List<Long>> apply(final AStream<Long> value) throws Throwable {
                                return aForStream(StreamUtil.head(value, 5)).toList();
                            }
                        });
                    }
                });
            }
        });
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
        final List<Long> r = doAsyncThrowable(new ACallable<List<Long>>() {
            @Override
            public Promise<List<Long>> call() throws Throwable {
                return aTry(new Timer()).run(new AFunction<List<Long>, Timer>() {
                    @Override
                    public Promise<List<Long>> apply(final Timer timer) throws Throwable {
                        return timer.fixedDelay(5, 5).map(new AFunction<List<Long>, AStream<Long>>() {
                            @Override
                            public Promise<List<Long>> apply(final AStream<Long> value) throws Throwable {
                                return aForStream(StreamUtil.head(value, 5)).toList();
                            }
                        });
                    }
                });
            }
        });
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
