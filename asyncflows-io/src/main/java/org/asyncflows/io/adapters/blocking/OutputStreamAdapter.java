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

package org.asyncflows.io.adapters.blocking;

import org.asyncflows.io.BufferOperations;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * The adapter for output stream.
 */
public class OutputStreamAdapter extends BlockingOutputAdapter<ByteBuffer, OutputStream, byte[]> {
    /**
     * The constructor.
     *
     * @param stream the output stream
     */
    public OutputStreamAdapter(final OutputStream stream) {
        super(stream);
    }

    @Override
    protected BufferOperations<ByteBuffer, byte[]> operations() {
        return BufferOperations.BYTE;
    }

    @Override
    protected void write(final OutputStream output, final byte[] array, final int offset, final int length)
            throws IOException {
        output.write(array, offset, length);
    }

    @Override
    protected void flush(final OutputStream output) throws IOException {
        output.flush();
    }
}
