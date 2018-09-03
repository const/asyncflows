package org.asyncflows.io.text;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.streams.AsyncStreams.aForStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the line decoder.
 */
public class LineStreamTest {

    @Test
    public void testLines() {
        final String input = "first\n\n\r\r\nfifth\u0085last";
        final List<String> result = doAsync(
                () -> aForStream(LineStream.readLines(StringInput.wrap(input))).toList());
        assertEquals(Arrays.asList("first", "", "", "", "fifth", "last"), result);
    }
}
