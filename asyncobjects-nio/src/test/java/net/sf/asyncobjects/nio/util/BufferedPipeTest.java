package net.sf.asyncobjects.nio.util;

import net.sf.asyncobjects.nio.AChannel;
import org.junit.Test;

import java.nio.CharBuffer;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.util.AllControl.aAll;
import static net.sf.asyncobjects.core.util.ResourceUtil.aTry;
import static org.junit.Assert.assertEquals;

/**
 * Test for buffered pipe.
 */
public class BufferedPipeTest {

    @Test
    public void charPipeTest() {
        final String source = "Test String";
        final String result = doAsync(() -> {
            final AChannel<CharBuffer> pipe = BufferedPipe.charPipe(2);
            return aAll(
                    () -> aTry(pipe.getOutput()).run(
                            value -> value.write(CharBuffer.wrap(source))
                    )
            ).and(
                    () -> aTry(pipe.getInput()).run(
                            value -> CharIOUtil.getContent(value, CharBuffer.allocate(3)))
            ).selectValue2();
        });
        assertEquals(source, result);
    }
}
