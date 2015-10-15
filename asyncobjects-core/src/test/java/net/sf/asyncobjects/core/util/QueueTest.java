package net.sf.asyncobjects.core.util;

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
        final int rc = doAsync(() -> {
            final AQueue<Integer> queue = new SimpleQueue<Integer>().export();
            queue.put(0);
            return aAll(
                    () -> aForRange(1, 10).map(value -> aLater(() -> queue.put(value))).toVoid()
            ).and(
                    () -> aForRange(-11, -1).all().map(
                            value -> queue.take()
                    ).leftFold(0, (value1, value2) -> aValue(value1 + value2))
            ).selectValue2();
        });
        assertEquals((11 * 10) / 2, rc);
    }
}
