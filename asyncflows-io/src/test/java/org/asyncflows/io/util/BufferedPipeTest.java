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

package org.asyncflows.io.util;

import org.asyncflows.io.AChannel;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;

import static org.asyncflows.core.AsyncContext.doAsync;
import static org.asyncflows.core.util.CoreFlowsAll.aAll;
import static org.asyncflows.core.util.CoreFlowsResource.aTry;
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
