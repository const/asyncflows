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

package org.asyncflows.core.streams;

import org.asyncflows.core.Promise;
import org.asyncflows.core.data.Maybe;
import org.asyncflows.core.function.ACloseable;
import org.asyncflows.core.function.AResolver;
import org.asyncflows.core.util.ChainedClosable;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aFailure;

/**
 * Build stream that works above some resource.
 *
 * @param <O> the stream element type
 * @param <I> the type of underlying resource
 */
public abstract class ChainedStreamBase<O, I extends ACloseable>
        extends ChainedClosable<I> implements AStream<O>, NeedsExport<AStream<O>> {
    /**
     * The observer for the stream outcome.
     */
    private final AResolver<Maybe<O>> streamOutcomeObserver = resolution -> {
        if (!resolution.isSuccess()) {
            invalidate(resolution.failure());
            startClosing();
        } else if (resolution.value() != null && resolution.value().isEmpty()) {
            startClosing();
        }
    };

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedStreamBase(final I wrapped) {
        super(wrapped);
    }

    @Override
    public Promise<Maybe<O>> next() {
        if (!isValidAndOpen()) {
            return invalidationPromise();
        }
        Promise<Maybe<O>> result;
        try {
            result = produce();
        } catch (Throwable t) {
            result = aFailure(t);
        }
        return result.listen(streamOutcomeObserver);
    }

    /**
     * @return the next produced element
     */
    protected abstract Promise<Maybe<O>> produce();

    @Override
    public AStream<O> export(final Vat vat) {
        return AStreamProxyFactory.createProxy(vat, this);
    }
}
