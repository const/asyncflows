package net.sf.asyncobjects.nio;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.adapters.Adapters;
import org.junit.Test;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.test.junit.AsyncAsserts.assertEqualsAsync;
import static org.junit.Assert.assertEquals;

/**
 * The test for IO utilities.
 */
public class IOUtilTest {
    @Test
    public void testDiscard() {
        final String sample = "Test String";
        final Long value = doAsync(new ACallable<Long>() {
            @Override
            public Promise<Long> call() throws Throwable {
                return assertEqualsAsync((long) sample.length(), new ACallable<Long>() {
                    @Override
                    public Promise<Long> call() throws Throwable {
                        return IOUtil.CHAR.discard(Adapters.getStringInput(sample), CharBuffer.allocate(2));
                    }
                });
            }
        });
        assertEquals(sample.length(), value.longValue());
    }
}
