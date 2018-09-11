/*
 * Copyright (c) 2018 Konstantin Plotnikov
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

import org.asyncflows.protocol.http.common.content.ContentUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for transfer encodings.
 */
public class TransferEncodingTest {
    @Test
    public void testParseSingle() {
        final List<TransferEncoding> parsed0 = TransferEncoding.parse(Collections.singletonList(
                ContentUtil.CHUNKED_ENCODING));
        assertEquals(1, parsed0.size());
        assertEquals(ContentUtil.CHUNKED_ENCODING, parsed0.get(0).getName());
        final List<TransferEncoding> parsed1 = TransferEncoding.parse(Collections.singletonList(
                "\t" + ContentUtil.CHUNKED_ENCODING + " "));
        assertEquals(1, parsed1.size());
        assertEquals(ContentUtil.CHUNKED_ENCODING, parsed1.get(0).getName());
        final List<TransferEncoding> parsed2 = TransferEncoding.parse(Arrays.asList(
                ContentUtil.CHUNKED_ENCODING, ContentUtil.GZIP_ENCODING + ", " + ContentUtil.DEFLATE_ENCODING));
        assertEquals(3, parsed2.size());
        assertEquals(ContentUtil.CHUNKED_ENCODING, parsed2.get(0).getName());
        assertEquals(ContentUtil.GZIP_ENCODING, parsed2.get(1).getName());
        assertEquals(ContentUtil.DEFLATE_ENCODING, parsed2.get(2).getName());
    }
}
