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

package org.asyncflows.io.adapters;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Tuple2;
import org.asyncflows.io.IOUtil;
import org.asyncflows.io.adapters.blocking.Adapters;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.CoreFlows.aValue;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test for adapters.
 */
public class AdaptersTest {
    /**
     * The data used for test.
     */
    private static final byte[] DATA5 = new byte[]{1, 2, 3, 4, 5};

    @Test
    public void testCopy() {
        final ByteBuffer buffer = ByteBuffer.allocate(2);
        final Tuple2<Long, byte[]> result = doCopyTest(true, buffer, DATA5);
        assertEquals(5L, result.getValue1().longValue());
        assertArrayEquals(DATA5, result.getValue2());
    }

    @Test
    public void testCopyDirect() {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        final Tuple2<Long, byte[]> result = doCopyTest(false, buffer, DATA5);
        assertEquals(5L, result.getValue1().longValue());
        assertArrayEquals(DATA5, result.getValue2());
    }

    @Test
    public void testCopyEmpty() {
        final ByteBuffer buffer = ByteBuffer.allocate(2);
        final byte[] empty = new byte[0];
        final Tuple2<Long, byte[]> result = doCopyTest(false, buffer, empty);
        assertEquals(0L, result.getValue1().longValue());
        assertArrayEquals(empty, result.getValue2());
    }

    @Test
    public void testStringCopy() {
        final CharBuffer buffer = CharBuffer.allocate(2);
        final String test = "Test String";
        final Tuple2<Long, String> tuple2 = doAsync(() -> {
            final Promise<String> second = new Promise<>();
            return aAll(
                    () -> aTry(() -> {
                        return aValue(Adapters.getStringInput(test));
                    }).andOther(() -> {
                        return aValue(Adapters.getStringOutput(second.resolver()));
                    }).run(
                            (value1, value2) -> IOUtil.CHAR.copy(value1, value2, false, buffer)
                    )
            ).andLast(() -> second);
        });
        assertEquals(test.length(), tuple2.getValue1().longValue());
        assertEquals(test, tuple2.getValue2());
    }

    /**
     * Execute copy test.
     *
     * @param autoFlush the value of autoFlush to use for copy
     * @param buffer    the buffer to test
     * @param inputData the input data
     * @return the result
     */
    private Tuple2<Long, byte[]> doCopyTest(final boolean autoFlush, final ByteBuffer buffer, final byte[] inputData) {
        return doAsync(() -> {
            final Promise<byte[]> array = new Promise<byte[]>();
            return aAll(
                    () -> aTry(() -> {
                        return aValue(Adapters.getByteArrayInput(inputData));
                    }).andOther(() -> {
                        return aValue(Adapters.getByteArrayOutput(array.resolver()));
                    }).run(
                            (input, output) -> IOUtil.BYTE.copy(input, output, autoFlush, buffer))
            ).andLast(() -> array);
        });
    }
}
