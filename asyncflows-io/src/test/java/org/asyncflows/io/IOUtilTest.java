package org.asyncflows.io;

import org.asyncflows.io.adapters.Adapters;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for IO utilities.
 */
public class IOUtilTest {
    @Test
    public void testDiscard() {
        final String sample = "Test String";
        final Long value = doAsync(() -> IOUtil.CHAR.discard(Adapters.getStringInput(sample), CharBuffer.allocate(2)));
        assertEquals(sample.length(), value.longValue());
    }
}
