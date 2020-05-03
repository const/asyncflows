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
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.ExportableComponent;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;

/**
 * The chained sink.
 *
 * @param <I> the sink element type
 * @param <U> the underlying resource type
 */
public abstract class ChainedSinkBase<I, U extends ACloseable> extends ChainedClosable<U>
        implements ASink<I>, ExportableComponent<ASink<I>> {
    /**
     * The finished promise.
     */
    private final Promise<Void> finished = new Promise<>();

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedSinkBase(final U wrapped) {
        super(wrapped);
    }

    @Override
    public Promise<Void> fail(final Throwable error) {
        invalidate(error);
        return aVoid();
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        if (finished.getOutcome() == null) {
            notifyFailure(finished.resolver(), throwable);
            startClosing();
        }
    }

    @Override
    public Promise<Void> finished() {
        return finished;
    }

    @Override
    public ASink<I> export(final Vat vat) {
        return ASinkProxyFactory.createProxy(vat, this);
    }
}
