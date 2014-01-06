package net.sf.asyncobjects.core.util;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AFunction;
import net.sf.asyncobjects.core.Promise;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.stream.Streams.aForRange;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static org.junit.Assert.assertEquals;

/**
 * The test for asynchronous queue.
 */
public class QueueTest {
    @Test
    public void test() {
        final int rc = doAsync(new ACallable<Integer>() {
            @Override
            public Promise<Integer> call() throws Throwable {
                final AQueue<Integer> queue = new SimpleQueue<Integer>().export();
                queue.put(0);
                return aAll(new ACallable<Void>() {
                    @Override
                    public Promise<Void> call() throws Throwable {
                        return aForRange(1, 10).map(new AFunction<Void, Integer>() {
                            @Override
                            public Promise<Void> apply(final Integer value) throws Throwable {
                                return aLater(new ACallable<Void>() {
                                    @Override
                                    public Promise<Void> call() throws Throwable {
                                        return queue.put(value);
                                    }
                                });
                            }
                        }).toUnit();
                    }
                }).and(new ACallable<Integer>() {
                    @Override
                    public Promise<Integer> call() throws Throwable {
                        return aForRange(-11, -1).all().map(new AFunction<Integer, Integer>() {
                            @Override
                            public Promise<Integer> apply(final Integer value) throws Throwable {
                                return queue.take();
                            }
                        }).leftFold(0, new AFunction2<Integer, Integer, Integer>() {
                            @Override
                            public Promise<Integer> apply(final Integer value1, final Integer value2) throws Throwable {
                                return aValue(value1 + value2);
                            }
                        });
                    }
                }).selectValue2();
            }
        });
        assertEquals((11 * 10) / 2, rc);
    }
}
