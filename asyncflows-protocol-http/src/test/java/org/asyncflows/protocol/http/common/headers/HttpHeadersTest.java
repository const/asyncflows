package org.asyncflows.protocol.http.common.headers;

import org.asyncflows.io.adapters.Adapters;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.io.util.CharIOUtil;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.asyncflows.core.Promise;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
