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
import org.asyncflows.core.data.Cell;
import org.asyncflows.core.function.ASupplier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aBoolean;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aMaybeEmpty;
import static org.asyncflows.core.CoreFlows.aMaybeValue;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.streams.AsyncStreams.aForArray;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqForCollect;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqForUnit;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqUntilValue;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeqWhile;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for sequential control constructs.
 */
public class SeqControlTest {

    /**
     * Check if failure is an exception and has a required method
     *
     * @param message the message
     * @param outcome the outcome to check
     */
    private static void assertFailureMessage(final String message, final Outcome<?> outcome) {
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals(message, outcome.failure().getMessage());
    }

    @Test
    public void testSeq() {
        final ArrayList<Integer> list = new ArrayList<>();
        final int rc = doAsync(() ->
                aSeq(() -> {
                    list.add(1);
                    return aValue(1);
                }).map(value -> {
                    list.add(value + 1);
                    throw new IllegalStateException();
                }).thenDo(() -> {
                    // never called
                    list.add(-1);
                    return aValue(-1);
                }).failed(value -> {
                    assertEquals(IllegalStateException.class, value.getClass());
                    list.add(3);
                    return aValue(42);
                }).finallyDo(() -> {
                    list.add(4);
                    return aVoid();
                }));
        assertEquals(42, rc);
        assertEquals(Arrays.asList(1, 2, 3, 4), list);
    }

    @Test
    public void testSeqSimple() {
        final String test = doAsync(() ->
                aSeq(
                        () -> aValue(42)
                ).map(
                        value -> aLater(() -> aValue("The answer is " + value))
                ).finallyDo(CoreFlows::aVoid));
        assertEquals("The answer is 42", test);
    }

    @Test
    public void testSeqFailed() {
        final Outcome<Integer> test = doAsync(() ->
                aSeq(
                        (ASupplier<Integer>) () -> aFailure(new IllegalStateException("test"))
                ).failedLast(
                        value -> aValue(42)
                ).toOutcomePromise());
        assertEquals(Outcome.success(42), test);
        final Outcome<Integer> test2 = doAsync(() ->
                aSeq(
                        (ASupplier<Integer>) () -> aFailure(new IllegalStateException("1"))
                ).failedLast(
                        value -> {
                            throw new IllegalStateException("2");
                        }
                ).toOutcomePromise());
        assertFailureMessage("2", test2);
    }

    @Test
    public void testSeqFinallyFailed() {
        final Outcome<Integer> test = doAsync(() ->
                aSeq(
                        (ASupplier<Integer>) () -> aFailure(new IllegalStateException("test"))
                ).finallyDo(
                        () -> aValue(42).toVoid()
                ).toOutcomePromise());
        assertFailureMessage("test", test);
        final Outcome<Integer> test2 = doAsync(() ->
                aSeq((ASupplier<Integer>) () -> {
                    throw new IllegalStateException("test");
                }).finallyDo(() -> aLater(() -> {
                    throw new IllegalStateException("finally");
                })).toOutcomePromise());
        assertFailureMessage("test", test2);
        final Outcome<Integer> test3 = doAsync(() ->
                aSeq(
                        () -> aValue(42)
                ).finallyDo(() -> {
                    throw new IllegalStateException("finally");
                }).toOutcomePromise());
        assertFailureMessage("finally", test3);
    }


    @Test
    public void testSeqForLoop() {
        final int rc = doAsync(() ->
                aForArray(0, 1, 2, 3, 4).leftFold(0,
                        (result, item) -> aValue(result + item)
                ));
        assertEquals(10, rc);
    }

    @Test
    public void testSeqForLoopSimple() {
        final int rc = doAsync(() -> {
            final int[] sum = new int[1];
            return aSeqForUnit(Arrays.asList(0, 1, 2, 3, 4), value -> {
                sum[0] += value;
                return aTrue();
            }).thenFlatGet(() -> aValue(sum[0]));
        });
        assertEquals(10, rc);
    }

    @Test
    public void testSeqWhile() {
        final int rc = doAsync(() -> {
            final int[] sum = new int[1];
            final int[] current = new int[1];
            return aSeqWhile(() -> {
                sum[0] += current[0];
                current[0]++;
                return aBoolean(current[0] <= 4);
            }).thenFlatGet(() -> aValue(sum[0]));
        });
        assertEquals(10, rc);
    }


    @Test
    public void testSeqUntilValue() {
        final int rc = doAsync(() -> {
            final int[] sum = new int[1];
            final int[] current = new int[1];
            return aSeqUntilValue(() -> {
                sum[0] += current[0];
                current[0]++;
                return current[0] <= 4 ? aMaybeEmpty() : aMaybeValue(sum[0]);
            });
        });
        assertEquals(10, rc);
    }


    @Test
    public void testSeqLoopFail() {
        final Cell<Boolean> flag = new Cell<>(true);
        final Outcome<Void> rc = doAsync(() ->
                aSeqWhile(() -> {
                    if (flag.getValue()) {
                        flag.setValue(false);
                        return aTrue();
                    } else {
                        return aLater(() -> {
                            throw new IllegalStateException("failed");
                        });
                    }
                }).toOutcomePromise());
        assertFailureMessage("failed", rc);
    }

    @Test
    public void seqForCollect() {
        final int rc = doAsync(() ->
                aSeqForCollect(Stream.of(1, 2, 3, 4),
                        e -> aValue(e + 1),
                        Collectors.summingInt((Integer e) -> e))
        );
        assertEquals(14, rc);
    }

}
