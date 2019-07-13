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

package org.asyncflows.core.vats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The batched vat. It it implements internally a queue of action that is optimized for
 * multiple-writers-single-reader usage pattern.
 */
public abstract class BatchedVat extends Vat {
    /**
     * The default size of batch to execute.
     */
    public static final int DEFAULT_BATCH_SIZE = 256;
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BatchedVat.class);
    /**
     * The batch size to execute.
     */
    private final int batchSize;
    /**
     * The queue.
     */
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    /**
     * If true, the vat is scheduled.
     */
    private final AtomicBoolean scheduled = new AtomicBoolean();

    /**
     * The constructor with default batch size.
     */
    protected BatchedVat() {
        this(DEFAULT_BATCH_SIZE);
    }

    /**
     * The constructor with the specified batch size.
     *
     * @param maxBatchSize the batch size
     */
    protected BatchedVat(final int maxBatchSize) {
        this.batchSize = maxBatchSize;
    }

    /**
     * Execute the action.
     *
     * @param action the action action
     */
    @Override
    public final void execute(final Runnable action) {
        queue.add(action);
        if (scheduled.compareAndSet(false, true)) {
            schedule();
        }
    }


    /**
     * Schedule vat for the further execution.
     */
    protected abstract void schedule();

    /**
     * Run a batch and leave.
     *
     * @return true if the vat is scheduled
     */
    @SuppressWarnings("squid:S3776")
    protected final boolean runBatch() {
        enter();
        boolean scheduledRun = false;
        try {
            for (int step = batchSize; step > 0; step--) {
                final Runnable action = queue.poll();
                if (action == null) {
                    break;
                }
                try {
                    action.run();
                } catch (Throwable t) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error while executing action: " + action, t);
                    }
                }
            }
        } finally {
            leave();
            scheduled.set(false);
            if (!queue.isEmpty()) {
                if (scheduled.compareAndSet(false, true)) {
                    schedule();
                }
                scheduledRun = true;
            }
        }
        return scheduledRun;
    }
}
