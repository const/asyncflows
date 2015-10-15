package net.sf.asyncobjects.core.util.control;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AsyncControl;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.data.Cell;
import net.sf.asyncobjects.core.vats.Vat;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static net.sf.asyncobjects.core.AsyncControl.aFailure;
import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aTrue;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.aVoid;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.stream.Streams.aForArray;
import static net.sf.asyncobjects.core.util.SeqControl.aSeq;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqForUnit;
import static net.sf.asyncobjects.core.util.SeqControl.aSeqLoopFair;
import static org.junit.Assert.assertEquals;

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
                ).finallyDo(AsyncControl::aVoid));
        assertEquals("The answer is 42", test);
    }

    @Test
    public void testSeqFailed() {
        final Outcome<Integer> test = doAsync(() ->
                aSeq(
                        (ACallable<Integer>) () -> aFailure(new IllegalStateException("test"))
                ).failedLast(
                        value -> aValue(42)
                ).toOutcomePromise());
        assertEquals(Outcome.success(42), test);
        final Outcome<Integer> test2 = doAsync(() ->
                aSeq(
                        (ACallable<Integer>) () -> aFailure(new IllegalStateException("1"))
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
                        (ACallable<Integer>) () -> aFailure(new IllegalStateException("test"))
                ).finallyDo(
                        () -> aValue(42).toVoid()
                ).toOutcomePromise());
        assertFailureMessage("test", test);
        final Outcome<Integer> test2 = doAsync(() ->
                aSeq((ACallable<Integer>) () -> {
                    throw new IllegalStateException("test");
                }).finallyDo(() -> aLater(Vat.current(), (ACallable<Void>) () -> {
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
            }).thenDo(() -> aValue(sum[0]));
        });
        assertEquals(10, rc);
    }


    @Test
    public void testSeqLoopFail() {
        final Cell<Boolean> flag = new Cell<>(true);
        final Outcome<Void> rc = doAsync(() ->
                aSeqLoopFair(() -> {
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

}
