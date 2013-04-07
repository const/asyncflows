package net.sf.asyncobjects.core.vats;

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
        executor.execute(new Runnable() {
            @Override
            public void run() {
                runBatch();
            }
        });
    }
}
