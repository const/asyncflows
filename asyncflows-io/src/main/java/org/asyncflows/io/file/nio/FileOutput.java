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

package org.asyncflows.io.file.nio;

import org.asyncflows.core.Promise;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.util.RequestQueue;
import org.asyncflows.core.vats.Vat;
import org.asyncflows.io.AOutput;
import org.asyncflows.io.AOutputProxyFactory;

import java.nio.ByteBuffer;

/**
 * File output.
 */
class FileOutput extends ChainedClosable<RandomAccessFile> implements AOutput<ByteBuffer>, ExportableComponent<AOutput<ByteBuffer>> {
    /**
     * The request queue.
     */
    private final RequestQueue requestQueue = new RequestQueue();
    /**
     * The append mode.
     */
    private final boolean append;
    /**
     * The current offset.
     */
    private long offset;

    /**
     * A constructor.
     *
     * @param file the file
     */
    FileOutput(RandomAccessFile file, boolean append) {
        super(file);
        this.append = append;
    }

    @Override
    public AOutput<ByteBuffer> export(Vat vat) {
        return AOutputProxyFactory.createProxy(vat, this);
    }

    @Override
    public Promise<Void> write(ByteBuffer buffer) {
        return requestQueue.run(() -> {
            ensureValidAndOpen();
            int before = buffer.remaining();
            return wrapped.write(buffer, append ? wrapped.getSize() : offset).map(t -> {
                offset += before - buffer.remaining();
                return t;
            });
        });
    }

    @Override
    public Promise<Void> flush() {
        return wrapped.force(true);
    }
}
