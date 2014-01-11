package net.sf.asyncobjects.nio.text;

import net.sf.asyncobjects.core.ACallable;
import net.sf.asyncobjects.core.Promise;
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
        final List<String> result = doAsync(new ACallable<List<String>>() {
            @Override
            public Promise<List<String>> call() throws Throwable {
                return aForStream(LineStream.readLines(StringInput.wrap(input))).toList();
            }
        });
        assertEquals(Arrays.asList("first", "", "", "", "fifth", "last"), result);
    }
}
