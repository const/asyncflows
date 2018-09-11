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

package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

/**
 * <p>A sink for values. The sink is inverse stream, and it is easy to convert from one to another.
 * The stream is pull or sequential side of processing, the sink is parallel or push side.</p>
 * <p>After sink is closed or invalidated, it discards all incoming values.</p>
 *
 * @param <T> the stream element type
 */
public interface ASink<T> extends ACloseable {
    /**
     * Put a value into the sink.
     *
     * @param value the value to put
     * @return the promise that resolves when sink is ready for the next value
     */
    Promise<Void> put(T value);

    /**
     * Write an error to sink, the sink is closed after this method since no more data
     * is expected.
     *
     * @param error the problem
     * @return the promise that resolves when sink is ready for the next value
     */
    Promise<Void> fail(Throwable error);

    /**
     * This method complements {@link ACloseable#close()} method on the {@link AStream} side.
     * this is a way to notify all interested parties that sink is not accepting items
     * anymore. The promise resolves to null in case of success, and fails in case of failure.
     *
     * @return the promise that resolves when sink is no more accepting the items.
     */
    Promise<Void> finished();
}
