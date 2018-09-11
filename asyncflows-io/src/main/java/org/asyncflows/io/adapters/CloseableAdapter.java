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

package org.asyncflows.io.adapters;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import java.io.Closeable;
import java.io.IOException;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The base adapter for closeable.
 *
 * @param <I> the stream type
 */
public abstract class CloseableAdapter<I extends Closeable> implements ACloseable {
    /**
     * The stream.
     */
    protected final I stream;
    /**
     * The closed flag.
     */
    private boolean closed;

    /**
     * The constructor.
     *
     * @param stream the stream
     */
    public CloseableAdapter(final I stream) {
        this.stream = stream;
    }

    @Override
    public final Promise<Void> close() {
        try {
            if (!closed) {
                closeStream(stream);
                closed = true;
            }
            return aVoid();
        } catch (IOException e) {
            return aFailure(e);
        }
    }

    /**
     * Close the stream. The close operation for the stream could differ depending on the
     * stream type. For example, for socket steams {@link java.net.Socket#shutdownInput()}
     * and {@link java.net.Socket#shutdownOutput()} should be used.
     *
     * @param streamToClose the stream to close
     * @throws IOException in case of the problem
     */
    protected void closeStream(final I streamToClose) throws IOException {
        streamToClose.close();
    }
}
