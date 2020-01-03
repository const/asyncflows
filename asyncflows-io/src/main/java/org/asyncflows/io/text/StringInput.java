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

package org.asyncflows.io.text;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.CloseableBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;

import java.nio.CharBuffer;

import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The input over character sequence.
 */
public class StringInput extends CloseableBase implements AInput<CharBuffer>, NeedsExport<AInput<CharBuffer>> {
    /**
     * The buffer with data.
     */
    private final CharBuffer data;

    /**
     * The character buffer with data.
     *
     * @param data the buffer with data
     */
    public StringInput(final CharBuffer data) {
        this.data = data;
    }

    /**
     * Wrap character sequence into the input.
     *
     * @param chars the characters to wrap
     * @return the input
     */
    public static AInput<CharBuffer> wrap(final CharSequence chars) {
        return new StringInput(CharBuffer.wrap(chars)).export();
    }

    @Override
    public Promise<Integer> read(final CharBuffer buffer) {
        ensureOpen();
        if (data.hasRemaining()) {
            return aValue(BufferOperations.CHAR.put(buffer, data));
        } else {
            return IOUtil.EOF_PROMISE;
        }
    }

    @Override
    public AInput<CharBuffer> export(final Vat vat) {
        return AInputProxyFactory.createProxy(vat, this);
    }
}
