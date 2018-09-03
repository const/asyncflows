package org.asyncflows.core.util;

import org.junit.jupiter.api.Test;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.AsyncControl.aLater;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.asyncflows.core.streams.AsyncStreams.aForRange;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                    () -> aForRange(1, 11).map(value -> aLater(() -> queue.put(value))).toVoid()
            ).and(
                    () -> aForRange(-11, 0).all().map(
                            value -> queue.take()
                    ).leftFold(0, (value1, value2) -> aValue(value1 + value2))
            ).selectValue2();
        });
        assertEquals((11 * 10) / 2, rc);
    }
}
