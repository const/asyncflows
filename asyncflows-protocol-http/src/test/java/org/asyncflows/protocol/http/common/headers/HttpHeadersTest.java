/*
 * Copyright (c) 2018-2020 Konstantin Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.asyncflows.protocol.http.common.headers;

import org.asyncflows.core.Promise;
import org.asyncflows.io.adapters.blocking.Adapters;
import org.asyncflows.io.util.ByteParserContext;
import org.asyncflows.protocol.http.common.HttpLimits;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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
                new ByteParserContext(Adapters.getByteArrayInput(data.getBytes(StandardCharsets.ISO_8859_1)), 1024),
                HttpLimits.MAX_HEADERS_SIZE);
    }
}
