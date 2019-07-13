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
import org.asyncflows.core.util.CloseableInvalidatingBase;
import org.asyncflows.core.util.NeedsExport;
import org.asyncflows.core.vats.Vat;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.Outcome.notifyFailure;
import static org.asyncflows.core.Outcome.notifySuccess;

/**
 * The base class for sink.
 *
 * @param <A> the element type
 */
public abstract class SinkBase<A> extends CloseableInvalidatingBase implements ASink<A>, NeedsExport<ASink<A>> {
    /**
     * The finished promise.
     */
    private final Promise<Void> finished = new Promise<>();

    @Override
    public Promise<Void> fail(final Throwable error) {
        invalidate(error);
        return aVoid();
    }

    @Override
    protected void onInvalidation(final Throwable throwable) {
        if (finished.isUnresolved()) {
            notifyFailure(finished.resolver(), throwable);
            startClosing();
        }
    }

    @Override
    protected Promise<Void> closeAction() {
        if (finished.isUnresolved()) {
            notifySuccess(finished.resolver(), null);
            startClosing();
        }
        return super.closeAction();
    }

    @Override
    public Promise<Void> finished() {
        return finished;
    }

    @Override
    public ASink<A> export(final Vat vat) {
        return ASinkProxyFactory.createProxy(vat, this);
    }
}
