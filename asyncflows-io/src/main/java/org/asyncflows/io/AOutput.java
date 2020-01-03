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

package org.asyncflows.io;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.function.ACloseable;

import java.nio.Buffer;

/**
 * An output based on byte buffer.
 *
 * @param <B> the buffer type
 */
@Asynchronous
public interface AOutput<B extends Buffer> extends ACloseable {

    /**
     * Write data to stream. Note that the data might be remaining in the buffer after write operation.
     * If data could not be completely processed, it should be saved in stream's buffer.
     *
     * @param buffer a buffer with data to write
     * @return a promise that resolves when data finished writing
     */
    Promise<Void> write(B buffer);

    /**
     * Flush as much data from buffers as possible. Note that some data could remain in the buffers if it could
     * not be readily converted (for example, incomplete code points in internal character buffer for
     * {@link org.asyncflows.io.text.EncoderOutput}).
     *
     * @return a promise that resolves when data finishes flushing
     */
    Promise<Void> flush();
}
