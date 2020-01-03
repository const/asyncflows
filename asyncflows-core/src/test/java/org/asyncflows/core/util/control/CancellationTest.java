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

package org.asyncflows.core.util.control;

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.util.Cancellation;
import org.asyncflows.core.util.SimpleQueue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.util.ControlUtils.rangeIterator;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqForUnit;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CancellationTest {

    @Test
    public void test() {
        final ArrayList<Integer> list = new ArrayList<>();
        doAsync(() -> {
            final SimpleQueue<Integer> queue = new SimpleQueue<>();
            final Cancellation cancellation = new Cancellation();
            return aAll(
                    // () -> aSeqWhile(() -> queue.take().map(t -> {
                    () -> aSeqWhile(() -> cancellation.run(queue::take).map(t -> {
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
                            () -> cancellation.run(() -> aFailure(new RuntimeException()))
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
