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

package org.asyncflows.io.util;

import org.asyncflows.io.IOUtil;
import org.asyncflows.io.adapters.Adapters;
import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.core.util.CoreFlowsResource;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.asyncflows.core.AsyncContext.doAsyncThrowable;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Simple tests for {@link GZipInput} with files actually created by the command-line GZip.
 */
public class GZipInputTest {

    @Test
    public void testBest() throws Throwable {
        final GZipHeader header = checkFile("gzip-data-best.txt.gz");
        assertEquals(GZipHeader.DEFLATE_COMPRESSION, header.getCompressionMethod());
        assertEquals(GZipHeader.MAXIMUM_COMPRESSION, header.getExtraFlags());
        assertEquals("gzip-data-best.txt", header.getName());
    }

    @Test
    public void testFast() throws Throwable {
        final GZipHeader header = checkFile("gzip-data-fast.txt.gz");
        assertEquals(GZipHeader.DEFLATE_COMPRESSION, header.getCompressionMethod());
        assertEquals(GZipHeader.FASTEST_COMPRESSION, header.getExtraFlags());
        assertEquals("gzip-data-fast.txt", header.getName());
    }

    @Test
    public void testNoName() throws Throwable {
        final GZipHeader header = checkFile("gzip-data-no-name.txt.gz");
        assertNull(header.getName());
    }

    /**
     * Check file and return header.
     *
     * @param file the file
     * @return the GZip header
     * @throws Throwable the problem
     */
    private GZipHeader checkFile(final String file) throws Throwable {
        final Tuple2<Long, GZipHeader> rc = doAsyncThrowable(() -> {
            final Promise<GZipHeader> header = new Promise<>();
            return aAll(
                    () -> CoreFlowsResource.aTryResource(
                            Adapters.getResource(GZipInputTest.class, file)
                    ).andChain(
                            value -> aValue(GZipInput.gunzip(value, header.resolver()))
                    ).run(
                            (value1, value2) -> IOUtil.BYTE.discard(value2, ByteBuffer.allocate(IOUtil.DEFAULT_BUFFER_SIZE))
                    )
            ).andLast(() -> header);
        });
        assertEquals(rc.getValue1().longValue(), 227);
        return rc.getValue2();
    }
}
