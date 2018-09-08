package org.asyncflows.core.util.control

import org.asyncflows.core.AsyncContext.doAsync
import org.asyncflows.core.CoreFlows.*
import org.asyncflows.core.Outcome.notifySuccess
import org.asyncflows.core.Promise
import org.asyncflows.core.util.CoreFlowsAny.aAny
import org.asyncflows.core.util.CoreFlowsAll.aAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinSampleTest {
    @Test
    fun testSuppressed() {
        val t = doAsync {
            val failure = Promise<Throwable>()
            val suppressed = Promise<Int>()
            aAll {
                aAny(true) {
                    aLater { aValue(1) }
                }.or {
                    aFailure(RuntimeException())
                }.or {
                    aValue(2)
                }.suppressed { v ->
                    notifySuccess(suppressed.resolver(), v)
                }.suppressedFailureLast { ex ->
                    notifySuccess<Throwable>(failure.resolver(), ex)
                }
            }.and {
                failure
            }.andLast {
                suppressed
            }
        }
        assertEquals(2, t.value1)
        assertEquals(RuntimeException::class.java, t.value2.javaClass)
        assertEquals(1, t.value3)
    }
}