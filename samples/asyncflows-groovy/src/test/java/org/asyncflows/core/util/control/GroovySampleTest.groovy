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

package org.asyncflows.core.util.control

import org.asyncflows.core.Promise
import org.junit.jupiter.api.Test

import static org.asyncflows.core.AsyncContext.doAsync
import static org.asyncflows.core.CoreFlows.*
import static org.asyncflows.core.Outcome.notifySuccess
import static org.asyncflows.core.util.CoreFlowsAny.aAny
import static org.asyncflows.core.util.CoreFlowsAll.aAll
import static org.junit.jupiter.api.Assertions.assertEquals

class GroovySampleTest {
    @Test
    void testSuppressed() {
        def t = doAsync {
            def failure = new Promise<Throwable>();
            def suppressed = new Promise<Integer>();
            aAll {
                aAny(true) {
                    aLater { aValue(1) }
                } or {
                    aFailure(new RuntimeException())
                } or {
                    aValue(2)
                } suppressed {
                    notifySuccess(suppressed.resolver(), it)
                } suppressedFailureLast {
                    notifySuccess(failure.resolver(), it);
                }
            } and {
                failure
            } andLast {
                suppressed
            }
        }
        assertEquals(2, t.getValue1().intValue());
        assertEquals(RuntimeException.class, t.getValue2().getClass());
        assertEquals(1, t.getValue3().intValue());
    }

}
