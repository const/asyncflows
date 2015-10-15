package net.sf.asyncobjects.core;

import org.junit.Test;

import static net.sf.asyncobjects.core.AsyncControl.aLater;
import static net.sf.asyncobjects.core.AsyncControl.aValue;
import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        } catch (IllegalStateException ex) {
            assertEquals("Test", ex.getMessage());
        }
    }

}
