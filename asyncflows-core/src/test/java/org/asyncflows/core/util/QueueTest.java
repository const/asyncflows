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

package org.asyncflows.core.util;

import org.junit.jupiter.api.Test;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.streams.AsyncStreams.aForRange;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
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
