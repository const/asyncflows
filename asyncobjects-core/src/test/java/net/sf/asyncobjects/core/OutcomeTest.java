package net.sf.asyncobjects.core;


import net.sf.asyncobjects.core.data.Cell;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Test for outcomes
 */
public class OutcomeTest {

    @Test
    public void testFailures() {
        final Outcome<Object> f1 = new Failure<Object>(new IllegalStateException("test"));
        assertNotNull(f1.toString());
        assertEquals(f1, f1);
        final Failure<Object> f2 = new Failure<Object>(f1.failure());
        assertEquals(f1, f2);
        assertFalse(f1.equals(new Cell<>().getValue()));
        assertEquals(f1.hashCode(), f2.hashCode());
        assertNotEquals(f1, new Failure<Object>(new IllegalArgumentException()));
        assertNotEquals(f1, new Failure<Object>(null));
        assertNotEquals(f1, Outcome.success(1));
        try {
            f1.force();
            fail("Should have failed");
        } catch (IllegalStateException ex) {
            assertEquals("test", ex.getMessage());
        } catch (Throwable ex) {
            fail("Wrong throwable: " + ex);
        }
        try {
            f1.value();
            fail("Should have failed");
        } catch (IllegalStateException ex) {
            assertEquals(f1.failure(), ex.getCause());
        }
        assertFalse(f1.isSuccess());
        final Failure<Integer> f3 = f2.toOtherType();
        assertSame(f3, f2);
    }
}
