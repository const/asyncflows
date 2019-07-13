/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

import org.asyncflows.core.AsyncContext.doAsync
import org.asyncflows.core.CoreFlows.*
import org.asyncflows.core.Outcome.notifySuccess
import org.asyncflows.core.Promise
import org.asyncflows.core.util.CoreFlowsAll.aAll
import org.asyncflows.core.util.CoreFlowsAny.aAny
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