/*
 * Copyright (c) 2018-2019 Konstantin Plotnikov
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

import org.asyncflows.core.Promise;
import org.asyncflows.io.AOutput;

import java.nio.ByteBuffer;

import static org.asyncflows.core.CoreFlows.aFailure;
import static org.asyncflows.core.CoreFlows.aTrue;
import static org.asyncflows.core.CoreFlows.aVoid;

/**
 * The context for binary generators. This helper class is used to generate structured binary output.
 * It helps to share buffer and other structures between different components.
 */
public class ByteGeneratorContext {
    /**
     * The output stream.
     */
    private final AOutput<ByteBuffer> output;
    /**
     * The buffer.
     */
    private final ByteBuffer buffer;
    /**
     * an exception in the case of writing failed.
     */
    private Throwable invalidation;
    /**
     * The write mode for the stream.
     */
    private boolean writeMode;

    /**
     * The constructor.
     *
     * @param output the output stream
     * @param buffer the buffer to use (it must be array-backed)
     */
    public ByteGeneratorContext(final AOutput<ByteBuffer> output, final ByteBuffer buffer) {
        this.output = output;
        this.buffer = buffer;
        if (!buffer.hasArray()) {
            throw new IllegalArgumentException("Only array-backed buffers are supported for byte generator context.");
        }
    }

    /**
     * The constructor.
     *
     * @param output     the output stream
     * @param bufferSize the buffer to use
     */
    public ByteGeneratorContext(final AOutput<ByteBuffer> output, final int bufferSize) {
        this(output, ByteBuffer.allocate(bufferSize));
    }


    /**
     * Ensure that the specified size is available in the buffer.
     *
     * @param size the size to make available.
     * @return the size
     */
    public Promise<Void> ensureAvailable(final int size) {
        ensureValid();
        if (buffer.remaining() >= size) {
            return aVoid();
        }
        if (buffer.capacity() < size) {
            throw new IllegalArgumentException("Buffer capacity is too small: " + buffer.capacity());
        }
        return send().thenFlatGet(() -> {
            if (buffer.remaining() < size) {
                throw new IllegalArgumentException("Unable to save data: " + buffer.remaining());
            }
            return aVoid();
        });
    }

    /**
     * Ensure that data is sent to the underlying stream.
     *
     * @return the promise that resolves to true when data is sent.
     */
    public Promise<Boolean> send() {
        ensureValid();
        if (isSendNeeded()) {
            writeMode = true;
            buffer.flip();
            return output.write(buffer).flatMapOutcome(value -> {
                buffer.compact();
                writeMode = false;
                if (value.isSuccess()) {
                    return aTrue();
                } else {
                    invalidation = value.failure();
                    return aFailure(invalidation);
                }
            });
        } else {
            return aTrue();
        }
    }

    /**
     * @return true if send is needed
     */
    public boolean isSendNeeded() {
        return buffer.position() > 0;
    }

    /**
     * Ensure that context did not fail and that there are no read operation in the progress.
     */
    private void ensureValid() {
        if (invalidation != null) {
            throw new IllegalStateException("The stream has been failed", invalidation);
        }
        if (writeMode) {
            throw new IllegalStateException("The method is called while write operation is in progress.");
        }
    }

    /**
     * @return the current buffer
     */
    public ByteBuffer buffer() {
        ensureValid();
        return buffer;
    }

    /**
     * @return the underlying output
     */
    public AOutput<ByteBuffer> getOutput() {
        return output;
    }
}
