package org.asyncflows.core.util.control;

import org.asyncflows.core.AsyncExecutionException;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple3;
import org.junit.jupiter.api.Test;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aLater;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.Outcome.notifySuccess;
import static org.asyncflows.core.util.CoreFlowsAny.aAny;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AnyControlTest {

    @Test
    public void testFirstCome() {
        int value = doAsync(() ->
                aAny(
                        () -> aLater(() -> aValue(1))
                ).orLast(
                        () -> aValue(2)
                )
        );
        assertEquals(2, value);
    }

    @Test
    public void testFailure() {
        try {
            doAsync(() ->
                    aAny(
                            () -> aLater(() -> aValue(1))
                    ).orLast(
                            () -> aFailure(new RuntimeException())
                    )
            );
            fail("Unreachable");
        } catch (AsyncExecutionException ex) {
            assertEquals(RuntimeException.class, ex.getCause().getClass());
        }
    }


    @Test
    public void testSuppressed() {
        Tuple3<Integer, Throwable, Integer> t = doAsync(
                () -> {
                    Promise<Throwable> failure = new Promise<>();
                    Promise<Integer> suppressed = new Promise<>();
                    return aAll(
                            () -> aAny(true,
                                    () -> aLater(() -> aValue(1))
                            ).or(
                                    () -> aFailure(new RuntimeException())
                            ).or(
                                    () -> aValue(2)
                            ).suppressed(v -> {
                                notifySuccess(suppressed.resolver(), v);
                            }).suppressedFailureLast(ex -> {
                                notifySuccess(failure.resolver(), ex);
                            })
                    ).and(
                            () -> failure
                    ).andLast(
                            () -> suppressed
                    );
                }
        );
        assertEquals(2, t.getValue1().intValue());
        assertEquals(RuntimeException.class, t.getValue2().getClass());
        assertEquals(1, t.getValue3().intValue());
    }

    @Test
    public void testPreferSuccess() {
        int value = doAsync(() ->
                aAny(true,
                        () -> aLater(() -> aValue(1))
                ).orLast(
                        () -> aFailure(new RuntimeException())
                )
        );
        assertEquals(1, value);
    }

}
