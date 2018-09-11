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

import org.asyncflows.core.CoreFlows;
import org.asyncflows.core.Promise;
import org.asyncflows.core.util.RequestQueue;

/**
 * Transform sink base where the next resource is a sink as well.
 *
 * @param <N> the next sink element type
 * @param <T> this sink element type
 */
public abstract class TransformSinkBase<N, T> extends ChainedSinkBase<T, ASink<N>> {
    /**
     * The request queue for this sink.
     */
    private final RequestQueue requests = new RequestQueue();

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected TransformSinkBase(final ASink<N> wrapped) {
        super(wrapped);
    }

    @Override
    public Promise<Void> fail(final Throwable error) {
        return requests.run(() -> failNext(error));
    }

    /**
     * @return the request queue for the next action (used to serialize next sink notification)
     */
    protected final RequestQueue requestQueue() {
        return requests;
    }

    /**
     * Fail next sink with the specified error.
     *
     * @param error the error to use
     * @return the failure
     */
    protected final Promise<Void> failNext(final Throwable error) {
        invalidate(error);
        return wrapped.fail(error);
    }

    /**
     * @return before close action
     */
    @Override
    protected Promise<Void> beforeClose() {
        return requests.run(CoreFlows::aVoid);
    }
}
