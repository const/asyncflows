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

package org.asyncflows.core.util;


import org.asyncflows.core.Promise;
import org.asyncflows.core.function.ACloseable;

import static org.asyncflows.core.CoreFlows.aVoid;
import static org.asyncflows.core.util.CoreFlowsSeq.aSeq;

/**
 * A Closeable instance above other closeable instance.
 *
 * @param <T> the underlying object type
 */
public abstract class ChainedClosable<T extends ACloseable> extends CloseableInvalidatingBase {
    /**
     * The wrapped closeable object.
     */
    protected final T wrapped;

    /**
     * The constructor from the underlying object.
     *
     * @param wrapped the underlying object
     */
    protected ChainedClosable(final T wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    protected Promise<Void> closeAction() {
        return aSeq(this::beforeClose).finallyDo(CoreFlowsResource.closeResourceAction(wrapped));
    }

    /**
     * @return before close action
     */
    protected Promise<Void> beforeClose() {
        return aVoid();
    }
}
