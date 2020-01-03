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

package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.annotations.Asynchronous;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ACloseable;

/**
 * Asynchronous stream of values. The stream elements could be success values or failures. It is up to stream
 * how it handles the failures. The stream generally accepts multiple close operations.
 *
 * @param <T> the stream element type
 */
@Asynchronous
public interface AStream<T> extends ACloseable {
    /**
     * <p>The next element in the stream. Note that the stream might contain errors. After EOF is returned,
     * all other reads will return EOF. If stream is closed, the next operation will throw an exception.</p>
     *
     * @return promise for option that is either empty (meaning that EOF is reached), or contains a value
     */
    Promise<Maybe<T>> next();
}
