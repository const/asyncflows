package org.asyncflows.core.util.control;

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.util.FailFast;
import org.asyncflows.core.util.SimpleQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.util.ControlUtils.rangeIterator;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqForUnit;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FailFastTest {

    @Test
    public void test() {
        ArrayList<Integer> list = new ArrayList<>();
        doAsync(() -> {
            SimpleQueue<Integer> queue = new SimpleQueue<>();
            FailFast failFast = new FailFast();
            return aAll(
                    // () -> aSeqWhile(() -> queue.take().map(t -> {
                    () -> aSeqWhile(() -> failFast.run(queue::take).map(t -> {
                        if (t == null) {
                            return false;
                        } else {
                            list.add(t);
                            return true;
                        }
                    }))
            ).andLast(
                    () -> aSeq(
                            () -> queue.put(1)
                    ).thenDo(
                            () -> queue.put(2)
                    ).thenDo(
                            // pause
                            () -> aSeqForUnit(rangeIterator(1, 10), t -> aLater(CoreFlows::aTrue))
                    ).thenDoLast(
                            () -> failFast.run(() -> aFailure(new RuntimeException()))
                    )
            ).mapOutcome(o -> {
                assertTrue(o.isFailure());
                assertEquals(RuntimeException.class, o.failure().getClass());
                return true;
            });
        });
        assertEquals(Arrays.asList(1, 2), list);
    }
}
