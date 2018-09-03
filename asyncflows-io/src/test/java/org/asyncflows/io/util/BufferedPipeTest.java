package org.asyncflows.io.util;

import org.asyncflows.io.AChannel;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.util.AsyncAllControl.aAll;
import static org.asyncflows.core.util.ResourceUtil.aTry;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
