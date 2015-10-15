package net.sf.asyncobjects.protocol.http.common.headers;

import net.sf.asyncobjects.core.Promise;
import net.sf.asyncobjects.nio.adapters.Adapters;
import net.sf.asyncobjects.nio.util.ByteParserContext;
import net.sf.asyncobjects.nio.util.CharIOUtil;
import net.sf.asyncobjects.protocol.http.common.HttpLimits;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static net.sf.asyncobjects.core.AsyncControl.doAsync;
import static org.junit.Assert.assertEquals;

/**
 * The test for http headers.
 */
public class HttpHeadersTest {

    @Test
    public void testReadEmpty() {
        final HttpHeaders rc = readHttpHeaders("\r\n");
        assertEquals(0, rc.getNames().size());
    }

    @Test
    public void testReadSimple() {
        final HttpHeaders rc = readHttpHeaders("Host: origin.example.com\r\n" +
                "Content-Type: video/h264\r\n" +
                "Content-Length: 1234567890987\r\n" +
                "Content-Length: 1234567890987\r\n" +
                "Expect: 100-continue\r\n" +
                "\r\n");
        assertEquals(4, rc.getNames().size());
        assertEquals(Collections.singletonList("100-continue"), rc.getHeaders("EXPECT"));
        assertEquals(Collections.singletonList("origin.example.com"), rc.getHeaders("host"));
        assertEquals(Arrays.asList("1234567890987", "1234567890987"), rc.getHeaders("Content-Length"));
    }


    /**
     * Read headers as string.
     *
     * @param text the text to read
     * @return the HTTP headers
     */
    private HttpHeaders readHttpHeaders(final String text) {
        return doAsync(() -> readHeadersAsync(text));
    }

    /**
     * Read the specified headers from the string (Latin-1).
     *
     * @param data the data to read
     * @return the read headers
     */
    public Promise<HttpHeaders> readHeadersAsync(final String data) {
        return HttpHeaders.readHeaders(
                new ByteParserContext(Adapters.getByteArrayInput(data.getBytes(CharIOUtil.ISO8859_1)), 1024),
                HttpLimits.MAX_HEADERS_SIZE);
    }
}
