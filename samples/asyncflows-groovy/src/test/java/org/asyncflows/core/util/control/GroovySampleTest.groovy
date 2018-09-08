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
