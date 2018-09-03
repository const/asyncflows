package org.asyncflows.core;


import org.asyncflows.core.data.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

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
        assertNotEquals(f1, new Cell<>().getValue());
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
