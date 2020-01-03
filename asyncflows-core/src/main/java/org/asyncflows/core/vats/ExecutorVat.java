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

package org.asyncflows.core.vats;

import java.util.concurrent.Executor;

/**
 * Executor vat.
 */
public final class ExecutorVat extends BatchedVat {
    /**
     * The executor to use to run the vat.
     */
    private final Executor executor;

    /**
     * The executor vat constructor.
     *
     * @param vatExecutor the executor
     * @param batchSize   the batch size
     */
    public ExecutorVat(final Executor vatExecutor, final int batchSize) {
        super(batchSize);
        this.executor = vatExecutor;
    }

    /**
     * The executor vat constructor.
     *
     * @param vatExecutor the executor
     */
    public ExecutorVat(final Executor vatExecutor) {
        this(vatExecutor, DEFAULT_BATCH_SIZE);
    }

    @Override
    protected void schedule() {
        executor.execute(this::runBatch);
    }
}
