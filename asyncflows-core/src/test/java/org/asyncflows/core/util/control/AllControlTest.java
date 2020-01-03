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

package org.asyncflows.core.util.control;

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Outcome;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.data.Tuple3;
import org.asyncflows.core.function.ASupplier;
import org.asyncflows.core.util.CoreFlowsAll;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.function.AsyncFunctionUtil.booleanSupplier;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsAll.aAllForCollect;
import static org.asyncflows.core.util.CoreFlowsAll.aPar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AllControlTest {
    @Test
    public void testAll2() {
        final Tuple2<String, Integer> rc = doAsync(() ->
                aAll(
                        () -> aValue("The answer")
                ).andLast(
                        () -> aLater(() -> aValue(42))
                ));
        assertEquals(Tuple2.of("The answer", 42), rc);
    }

    @Test
    public void testAll2Map() {
        final Tuple2<String, Integer> rc = doAsync(() ->
                aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).map((a, b) -> aValue(Tuple2.of(a, b))));
        assertEquals(Tuple2.of("The answer", 42), rc);
    }

    @Test
    public void testPar2Map() {
        final Tuple2<String, Integer> rc = doAsync(() ->
                aPar(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).map((a, b) -> aValue(Tuple2.of(a, b))));
        assertEquals(Tuple2.of("The answer", 42), rc);
    }

    @Test
    public void testAll2Failures() {
        final Outcome<Tuple2<String, Integer>> rc1 = doAsync(() ->
                aAll(
                        () -> CoreFlows.<String>aFailure(new IllegalStateException("1"))
                ).andLast(
                        () -> CoreFlows.<Integer>aFailure(new IllegalStateException("2"))
                ).toOutcomePromise());
        assertFalse(rc1.isSuccess());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals("1", rc1.failure().getMessage());
        final Outcome<Tuple2<String, Integer>> rc2 = doAsync(() ->
                aAll(
                        () -> aValue("1")
                ).andLast((ASupplier<Integer>) () -> {
                    throw new IllegalStateException("2");
                }).toOutcomePromise());
        assertFalse(rc2.isSuccess());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals("2", rc2.failure().getMessage());
    }

    @Test
    public void testAll2SelectZip() {
        final Tuple2<String, Integer> rc = doAsync(new ASupplier<Tuple2<String, Integer>>() {
            @Override
            public Promise<Tuple2<String, Integer>> get() {
                return aAll(
                        () -> partialAll().selectValue1()
                ).and(
                        () -> partialAll().selectValue2()
                ).map((value1, value2) -> aValue(Tuple2.of(value1, value2)));
            }

            private CoreFlowsAll.AllBuilder.AllBuilder2<String, Integer> partialAll() {
                return aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                );
            }
        });
        assertEquals(Tuple2.of("The answer", 42), rc);
    }


    @Test
    public void testAll3SelectZip() {
        final Tuple3<String, Integer, Boolean> rc = doAsync(new ASupplier<Tuple3<String, Integer, Boolean>>() {
            @Override
            public Promise<Tuple3<String, Integer, Boolean>> get() {
                return aAll(() -> partialAll().selectValue1()
                ).and(
                        () -> partialAll().selectValue2()
                ).and(
                        () -> partialAll().selectValue3()
                ).map((value1, value2, value3) -> aValue(Tuple3.of(value1, value2, value3)));
            }

            private CoreFlowsAll.AllBuilder.AllBuilder3<String, Integer, Boolean> partialAll() {
                return aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).and(
                        () -> aLater(booleanSupplier(false))
                );
            }
        });
        assertEquals(Tuple3.of("The answer", 42, false), rc);
    }


    @Test
    public void testAll3() {
        final Tuple3<String, Integer, Boolean> rc = doAsync(() ->
                aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).andLast(
                        () -> aValue(true)
                ));
        assertEquals(Tuple3.of("The answer", 42, true), rc);
    }

    @Test
    public void testAllZip3() {
        final String rc = doAsync(() ->
                aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).and(
                        () -> aValue("is")
                ).map((noun, definition, verb) -> aValue(noun + " " + verb + " " + definition)));
        assertEquals("The answer is 42", rc);
    }

    @Test
    public void allForCollect() {
        final int rc = doAsync(() ->
                aAllForCollect(Stream.of(1, 2, 3, 4),
                        e -> aValue(e + 1),
                        Collectors.summingInt((Integer e) -> e))
        );
        assertEquals(14, rc);
    }
}
