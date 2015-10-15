package net.sf.asyncobjects.nio.text;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static net.sf.asyncobjects.core.stream.Streams.aForStream;
import static org.junit.Assert.assertEquals;

/**
 * Test for the line decoder.
 */
public class LineStreamrTest {

    @Test
    public void testLines() {
        final String input = "first\n\n\r\r\nfifth\u0085last";
        final List<String> result = doAsync(
                () -> aForStream(LineStream.readLines(StringInput.wrap(input))).toList());
        assertEquals(Arrays.asList("first", "", "", "", "fifth", "last"), result);
    }
}
