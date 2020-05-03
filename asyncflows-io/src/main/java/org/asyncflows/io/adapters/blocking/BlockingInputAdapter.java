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

package org.asyncflows.io.adapters.blocking;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AInput;
import org.asyncflows.io.AInputProxyFactory;
import org.asyncflows.io.BufferOperations;
import org.asyncflows.io.IOUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aValue;

/**
 * The simple adapter for JDK input. The adapter is not thread safe.
 *
 * @param <B> the buffer type
 * @param <I> the input type
 * @param <A> the array type
 */
public abstract class BlockingInputAdapter<B extends Buffer, I extends Closeable, A>
        extends CloseableAdapter<I> implements AInput<B>, ExportableComponent<AInput<B>> {
    /**
     * The data array.
     */
    private A dataArray;

    /**
     * The constructor.
     *
     * @param stream the input stream
     */
    public BlockingInputAdapter(final I stream) {
        super(stream);
    }

    /**
     * @return the buffer operations
     */
    protected abstract BufferOperations<B, A> operations();

    /**
     * Read data from input into the array.
     *
     * @param input  the input to read from
     * @param array  the array
     * @param offset the offset in array
     * @param length the max length to read
     * @return the read size
     * @throws IOException in case of IO problem
     */
    protected abstract int read(I input, A array, int offset, int length) throws IOException;

    @Override
    public final Promise<Integer> read(final B buffer) {
        if (buffer.remaining() == 0) {
            return aValue(0);
        }
        final BufferOperations<B, A> operations = operations();
        A b;
        int offset;
        int length;
        if (buffer.hasArray()) {
            b = operations.array(buffer);
            offset = buffer.arrayOffset() + buffer.position();
            length = buffer.remaining();
        } else {
            if (dataArray == null) {
                dataArray = operations.allocate(IOUtil.DEFAULT_BUFFER_SIZE);
            }
            b = dataArray;
            offset = 0;
            length = Math.min(IOUtil.DEFAULT_BUFFER_SIZE, buffer.remaining());
        }
        try {
            final int rc = read(stream, b, offset, length);
            if (rc > 0) {
                if (buffer.hasArray()) {
                    buffer.position(buffer.position() + rc);
                } else {
                    operations.put(buffer, b, offset, rc);
                }
            }
            return aValue(rc);
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    @Override
    public final AInput<B> export(final Vat vat) {
        return AInputProxyFactory.createProxy(vat, this);
    }

    /**
     * @return the proxy for blocking stream
     */
    public final AInput<B> exportBlocking() {
        return BlockingIOExportUtil.exportBlocking(this);
    }
}
