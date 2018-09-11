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
