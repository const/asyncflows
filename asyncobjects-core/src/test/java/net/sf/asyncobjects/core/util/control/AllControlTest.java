package net.sf.asyncobjects.core.util.control;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.AsyncControl;
import net.sf.asyncobjects.core.CoreFunctionUtil;
import net.sf.asyncobjects.core.Outcome;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.core.data.Tuple2;
import net.sf.asyncobjects.core.data.Tuple3;
import net.sf.asyncobjects.core.util.AllControl;
import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    public void testAll2Failures() {
        final Outcome<Tuple2<String, Integer>> rc1 = doAsync(() ->
                aAll(
                        () -> AsyncControl.<String>aFailure(new IllegalStateException("1"))
                ).andLast(
                        () -> AsyncControl.<Integer>aFailure(new IllegalStateException("2"))
                ).toOutcomePromise());
        assertFalse(rc1.isSuccess());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals("1", rc1.failure().getMessage());
        final Outcome<Tuple2<String, Integer>> rc2 = doAsync(() ->
                aAll(
                        () -> aValue("1")
                ).andLast((ACallable<Integer>) () -> {
                    throw new IllegalStateException("2");
                }).toOutcomePromise());
        assertFalse(rc2.isSuccess());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertEquals("2", rc2.failure().getMessage());
    }

    @Test
    public void testAll2SelectZip() {
        final Tuple2<String, Integer> rc = doAsync(new ACallable<Tuple2<String, Integer>>() {
            @Override
            public Promise<Tuple2<String, Integer>> call() throws Exception {
                return aAll(
                        () -> partialAll().selectValue1()
                ).and(
                        () -> partialAll().selectValue2()
                ).unzip((value1, value2) -> aValue(Tuple2.of(value1, value2)));
            }

            private AllControl.AllBuilder.AllBuilder2<String, Integer> partialAll() {
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
        final Tuple3<String, Integer, Boolean> rc = doAsync(new ACallable<Tuple3<String, Integer, Boolean>>() {
            @Override
            public Promise<Tuple3<String, Integer, Boolean>> call() throws Exception {
                return aAll(() -> partialAll().selectValue1()
                ).and(
                        () -> partialAll().selectValue2()
                ).and(
                        () -> partialAll().selectValue3()
                ).unzip((value1, value2, value3) -> aValue(Tuple3.of(value1, value2, value3)));
            }

            private AllControl.AllBuilder.AllBuilder3<String, Integer, Boolean> partialAll() {
                return aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).and(
                        () -> aLater(CoreFunctionUtil.booleanCallable(false))
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
                ).unzip((noun, definition, verb) -> aValue(noun + " " + verb + " " + definition)));
        assertEquals("The answer is 42", rc);
    }
}
