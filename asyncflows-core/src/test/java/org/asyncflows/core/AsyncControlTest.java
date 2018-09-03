package org.asyncflows.core;

import org.junit.jupiter.api.Test;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.AsyncControl.aLater;
import static org.asyncflows.core.AsyncControl.aValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AsyncControlTest {
    @Test
    public void testSimple() {
        final int rc = doAsync(() -> aValue(42));
        assertEquals(42, rc);
    }

    @Test
    public void testSimpleLater() {
        final int rc = doAsync(() -> aLater(() -> aValue(42)));
        assertEquals(42, rc);
    }

    @Test
    public void testThrowLater() {
        try {
            doAsync(() -> aLater(() -> {
                throw new IllegalStateException("Test");
            }));
            fail("the doAsync should throw an exception!");
        } catch (AsyncExecutionException ex) {
            assertEquals(IllegalStateException.class, ex.getCause().getClass());
            assertEquals("Test", ex.getCause().getMessage());
        }
    }

}
