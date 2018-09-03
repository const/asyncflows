package org.asyncflows.core.util.sample;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqForCollect;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqForUnit;
import static org.asyncflows.core.util.ControlUtils.rangeIterator;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for asynchronous queue.
 */
public class TestQueueTest {
    @Test
    public void test() {
        final int rc = doAsync(() -> {
            final ATestQueue<Integer> queue = new TestQueue<Integer>().export();
            return aAll(() -> aSeqForUnit(rangeIterator(0, 10), i -> {
                queue.put(i + 1);
                return aTrue();
            })).and(() -> aSeqForCollect(rangeIterator(0, 10),
                    i -> queue.take(),
                    Collectors.summingInt((Integer i) -> i))
            ).selectValue2();
        });
        assertEquals((11 * 10) / 2, rc);
    }

}
